package com.evofun.gameservice.feature.game.domain.core;

import com.evofun.gameservice.GamePhaseUI;
import com.evofun.gameservice.feature.game.api.dto.response.SeatResponse;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessage;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessageSenderImpl;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessageType;
import com.evofun.gameservice.feature.game.domain.core.timer.DecisionTimeObserver;
import com.evofun.gameservice.feature.game.domain.core.timer.TimerService;
import com.evofun.gameservice.feature.game.domain.core.timer.TimerType;
import com.evofun.gameservice.feature.game.domain.model.*;
import com.evofun.gameservice.feature.game.domain.model.enums.*;
import com.evofun.gameservice.feature.game.exception.BlackjackException;
import com.evofun.gameservice.feature.game.infrastructure.mapper.DealerMapper;
import com.evofun.gameservice.feature.game.infrastructure.mapper.PlayerMapper;
import com.evofun.gameservice.feature.game.infrastructure.mapper.SeatMapper;
import com.evofun.gameservice.feature.game.infrastructure.mapper.TableMapper;
import com.evofun.gameservice.feature.game.utils.PlayerRegistry;
import com.evofun.gameservice.feature.result.application.model.GameResultSnapshot;
import com.evofun.gameservice.feature.result.application.model.PlayerSnapshot;
import com.evofun.gameservice.feature.result.application.model.SeatSnapshot;
import com.evofun.gameservice.shared.error.ErrorCode;
import com.evofun.gameservice.shared.error.ErrorDto;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GameEngine {
    private static final Logger logger = LoggerFactory.getLogger(GameEngine.class);

    private static final int TIME_FOR_DECISION = 1000;
    private static final int TIME_FOR_RESULT_ANNOUNCEMENT = 5000;
    private static final int TIME_BETWEEN_CARDS = 1000;
    private static final int COUNT_OF_INITIAL_CARDS = 2;

    private SmallDeck smallDeckObject = new SmallDeck();
    private List<Card> gameDeck = null;

    private Dealer dealer;
    private Table table;

    private TimerService timerService;
    private DecisionTimeObserver decisionTimeObserver;

    private GameDecision gameDecisionField = null;

    @Getter
    @Setter
    private boolean isGameRunning = false;

    private final WsMessageSenderImpl messageSenderImpl;

    private List<Player> gamePlayers = null;//for money management
    private List<Seat> gameSeats;

    private GamePhaseUI gameStatusForInterface = GamePhaseUI.EMPTY_TABLE;

    public GameDecision getGameDecisionField() {
        GameDecision gameDecision = gameDecisionField;

        gameDecisionField = null;

        return gameDecision;
    }

    public void setGameDecisionField(GameDecision gameDecision) {
        if (gameDecision.equals(GameDecision.HIT) ||
                gameDecision.equals(GameDecision.DOUBLE_DOWN) ||
                gameDecision.equals(GameDecision.SPLIT) ||
                gameDecision.equals(GameDecision.CASH_OUT) ||
                gameDecision.equals(GameDecision.STAND)) {

            gameDecisionField = gameDecision;
            timerService.stop(TimerType.DECISION_TIME);
        } else {
            System.err.println("Server got invalid decision: " + gameDecision);
        }
    }

    public void changeGameStatusForInterface(GamePhaseUI status) {
        gameStatusForInterface = status;
        messageSenderImpl.broadcast(new WsMessage<>(gameStatusForInterface, WsMessageType.E_GAME_STATUS_FOR_INTERFACE));
    }

    public GameEngine(Table table, WsMessageSenderImpl messageSenderImpl, TimerService timerService, PlayerRegistry playerRegistry, DecisionTimeObserver decisionTimeObserver) {
        this.table = table;
        this.messageSenderImpl = messageSenderImpl;
        this.timerService = timerService;
        this.decisionTimeObserver = decisionTimeObserver;
        gamePlayers = playerRegistry.getPlayers();
    }

    public GameResultSnapshot startGame() {
        if (table.isGame()) {
            return null;
        } else {
            table.setGame(true);
            isGameRunning = true;
        }

        timerService.stop(TimerType.BETTING_TIME);

        preparePlayersAndSeatsForGame();
        for (Player player : gamePlayers) {
            messageSenderImpl.sendToClient(
                    player.getUserId(),
                    new WsMessage<>(PlayerMapper.toResponse(player), WsMessageType.PLAYER_DATA)
            );
        }

        gameSeats = gameSeats.stream()
                .sorted(Comparator.comparingInt(Seat::getSeatNumber))
                .collect(Collectors.toList());

        table.setDealer(new Dealer());
        dealer = table.getDealer();
        messageSenderImpl.broadcast(new WsMessage<>(TableMapper.toDto(table), WsMessageType.GAME_STARTED));//mb send after resetGameResultStatus
        messageSenderImpl.broadcast(new WsMessage<>(DealerMapper.toDto(dealer), WsMessageType.DEALER));//TODO mb not to send the dealer (//mb send after resetGameResultStatus)

        if (gameDeck == null) {
            gameDeck = new ArrayList<>(smallDeckObject.getSmallDeck());//TODO mb change smth here
            Collections.shuffle(gameDeck);
        }

        for (Seat seat : gameSeats) {
            seat.resetGameResultStatus();
        }

        System.out.println("Bets are closed, good luck!");

        changeGameStatusForInterface(GamePhaseUI.DEALING_CARDS);

        System.out.println("\nInitial cards:");
        for (int i = 1; i <= COUNT_OF_INITIAL_CARDS; ++i) {
            for (Seat seat : gameSeats) {

                Card card = takeCard();

                seat.calculateScore(card);

                if (seat.getMainScore() == 21) {
                    System.out.println(card.getInitial() + " of " + card.getSuit() + " was " +
                            "dealt to player on seat #" + seat.getSeatNumber() + ", score - BLACKJACK (" + seat.getMainScore() + ")");

                    seat.setRoundResult(ProgressRoundResult.BLACKJACK);
                    SeatResponse seatResponse = SeatMapper.toDto(seat);
                    messageSenderImpl.broadcast(new WsMessage<>(seatResponse, WsMessageType.GAME_SEAT_UPDATED));

                } else {
                    System.out.println(card.getInitial() + " of " + card.getSuit() + " was " +
                            "dealt to player on seat #" + seat.getSeatNumber() + ", score = " + seat.getMainScore());
                    SeatResponse seatResponse = SeatMapper.toDto(seat);
                    messageSenderImpl.broadcast(new WsMessage<>(seatResponse, WsMessageType.GAME_SEAT_UPDATED));
                }

                try {
                    Thread.sleep(TIME_BETWEEN_CARDS);//here was 1s
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            Card card = takeCard();

            if (i == 1) {
                dealer.calculateScore(card);
                System.out.println(card.getInitial() + " of " + card.getSuit() + " was " +
                        "dealt to '" + dealer.getNickName() + "', score = " + dealer.getScore());

                messageSenderImpl.broadcast(new WsMessage<>(DealerMapper.toDto(dealer), WsMessageType.DEALER));

            } else {//TODO do it more beautiful and smarter
                System.out.println("hidden card was dealt to '" + dealer.getNickName() + "'" +
                        ", score = " + dealer.getCurrentCardInHandByIndex(0).getCoefficient() + "+");

                dealer.setHiddenCard(card);
                messageSenderImpl.broadcast(new WsMessage<>(DealerMapper.toDto(dealer), WsMessageType.DEALER));
            }

            try {
                Thread.sleep(TIME_BETWEEN_CARDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        changeGameStatusForInterface(GamePhaseUI.CARDS_WERE_DEALT);

        changeGameStatusForInterface(GamePhaseUI.DECISION_TIME);

        System.out.println("\nPlayer's decisions:");

        for (Seat curSeat : gameSeats) {

            while (curSeat.getMainScore() < 21) {

                GameDecision firstGameDecision = gettingDecision(curSeat);

                try {//имитация того, что дилер берёт карту
                    Thread.sleep(TIME_BETWEEN_CARDS);//1
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                if (firstGameDecision == null) {
                    logger.error("Smth went wrong and decision is null");
                    new RuntimeException("Smth went wrong and decision is null");
                    return null;
                }

                if (firstGameDecision.equals(GameDecision.STAND)) {
                    curSeat.setLastGameDecision(firstGameDecision);
                    SeatResponse seatResponse = SeatMapper.toDto(curSeat);
                    messageSenderImpl.broadcast(new WsMessage<>(seatResponse, WsMessageType.GAME_SEAT_UPDATED));
                    System.out.println(curSeat.getPlayerId() + " decided to " + firstGameDecision);

                    System.out.println(curSeat.getPlayerId() + " is standing on " + curSeat.getMainScore());

                    curSeat.setRoundResult(ProgressRoundResult.STAND);
                    break;

                } else if (firstGameDecision.equals(GameDecision.HIT)) {
                    curSeat.setLastGameDecision(firstGameDecision);

                    System.out.println(curSeat.getPlayerId() + " decided to " + firstGameDecision);

                    GameDecision nextGameDecision;
                    boolean isStand = false;

                    do {
                        Card card = takeCard();

                        curSeat.calculateScore(card);

                        System.out.println(card.getInitial() + " of " + card.getSuit() + " was " +
                                "dealt to '" + curSeat.getPlayerId() + "', score = " + curSeat.getMainScore());

                        SeatResponse seatResponse = SeatMapper.toDto(curSeat);
                        messageSenderImpl.broadcast(new WsMessage<>(seatResponse, WsMessageType.GAME_SEAT_UPDATED));

                        if (curSeat.getMainScore() < 21) {
                            do {
                                System.out.print(curSeat.getPlayerId() + " has " + curSeat.getMainScore() + ", what is your next decision? (hit, cash-out, stand) - ");

                                nextGameDecision = gettingDecision(curSeat);
                                //TODO it requires a check for NULL
                                curSeat.setLastGameDecision(nextGameDecision);

                                if (nextGameDecision.equals(GameDecision.STAND)) {
                                    System.out.println(curSeat.getPlayerId() + " is standing on " + curSeat.getMainScore());
                                    isStand = true;

                                    curSeat.setRoundResult(ProgressRoundResult.STAND);
                                    break;
                                } else if (nextGameDecision.equals(GameDecision.CASH_OUT)) {
                                    System.out.println(curSeat.getPlayerId() + " CASHOUT");
                                    isStand = true;
                                    curSeat.setRoundResult(FinalRoundResult.CASH_OUT);
                                    seatResponse = SeatMapper.toDto(curSeat);
                                    messageSenderImpl.broadcast(new WsMessage<>(seatResponse, WsMessageType.GAME_SEAT_UPDATED));

                                    break;
                                } else if (nextGameDecision.equals(GameDecision.DOUBLE_DOWN)) {
                                    ErrorDto errorDto = new ErrorDto(ErrorCode.GAME_RULE_VIOLATION, null, "You can not DOUBLE DOWN now, choose other option!", null);
                                    messageSenderImpl.sendToClient(curSeat.getPlayerId(), new WsMessage<>(errorDto, WsMessageType.ERROR));
                                }
                            } while (!isValidNextDecision(nextGameDecision));
                        } else if (curSeat.getMainScore() == 21) {
                            isStand = true;
                            curSeat.setRoundResult(ProgressRoundResult.STAND);
                        } else {
                            isStand = true;
                            curSeat.setRoundResult(FinalRoundResult.LOSE);
                        }

                    } while (!isStand);

                    break;

                } else if (firstGameDecision.equals(GameDecision.DOUBLE_DOWN)) {
                    //money
                    Player curPlayer = null;
                    for (Player player : gamePlayers) {
                        if (player.getUserId().equals(curSeat.getPlayerId())) {
                            curPlayer = player;
                        }
                    }

                    if (curPlayer == null) {
                        logger.error("curPlayer is null");
                        return null;
                    }

                    Seat tmpSeat = null;
                    int tmpInd = -1;
                    for (Seat s : curPlayer.getSeats()) {
                        if (curSeat.getSeatNumber() == s.getSeatNumber()) {
                            tmpSeat = s;
                            tmpInd = curPlayer.getSeats().indexOf(s);
                        }
                    }

                    if (tmpSeat == null || tmpInd == -1) {
                        logger.error("tmpSeat or tmpInd is wrong");
                        return null;
                    }

                    curPlayer.getSeats().set(tmpInd, curSeat);

                    curSeat.setLastGameDecision(firstGameDecision);
                    System.out.println(curSeat.getPlayerId() + " decided to " + firstGameDecision);

                    Card card = takeCard();

                    curSeat.calculateScore(card);

                    SeatResponse seatResponse = SeatMapper.toDto(curSeat);
                    messageSenderImpl.broadcast(new WsMessage<>(seatResponse, WsMessageType.GAME_SEAT_UPDATED));

                    System.out.println(firstGameDecision + " for " + curSeat.getPlayerId());
                    System.out.println(card.getInitial() + " of " + card.getSuit() + " was " +
                            "dealt to '" + curSeat.getPlayerId() + "'");

                    if (curSeat.getMainScore() < 21) {
                        System.out.println(curSeat.getPlayerId() + " has " + curSeat.getMainScore());
                        curSeat.setRoundResult(ProgressRoundResult.STAND);
                    } else if (curSeat.getMainScore() == 21) {
                        curSeat.setRoundResult(ProgressRoundResult.STAND);
                    } else {
                        curSeat.setRoundResult(FinalRoundResult.LOSE);
                    }

                    break;

                }/* else if (firstGameDecision.equals(GameDecision.SPLIT)) {

                    //TODO develop this split decision

                    seat.setLastGameDecision(firstGameDecision);
                    System.out.println(seat.getUserId() + " decided to " + firstGameDecision);
                    System.out.println(firstGameDecision + " for " + seat.getUserId());

                    //replacing 1st cards
                    seat.getAdditionalHandForSplit().add(seat.getMainHand().getLast());//take card from main hand and put in additional hand
                    seat.getMainHand().removeLast();//remove 'put card in additional hand' from main hand

                    //changing mainScore (according only 1 card in hand)
                    seat.changeMainScore(-seat.getAdditionalHandForSplit().getLast().getCoefficient());//change mainScore (minus last card)

                    //changing additionalScore (according only 1 card in hand)
                    seat.changeAdditionalScore(seat.getAdditionalHandForSplit().getLast().getCoefficient());

                    //adding one more card in each hand (initial cards for split)
                    seat.getMainHand().add(gameDeck.removeLast());
                    seat.getAdditionalHandForSplit().add(gameDeck.removeLast());

                    //changing both mainScore and additionalScore (according full initial cards)
                    seat.changeMainScore(seat.getMainHand().getLast().getCoefficient());
                    seat.changeAdditionalScore(seat.getAdditionalHandForSplit().getLast().getCoefficient());

                    //i need to take the player with this seat and change his old seat to new one
                    Player splitPlayer = null;
                    for (Player p : playersInGameSession) {
                        if (p.getUserId().equals(seat.getUserId())) {
                            splitPlayer = p;
                        }
                        break;
                    }

                    if (splitPlayer == null) {
                        logger.error("splitPlayer is null");
                        return null;
                    }

                    int splitInd = -1;
                    for (Seat s : splitPlayer.getSeats()) {
                        if (seat.getSeatNumber() == s.getSeatNumber()) {
                            splitInd = splitPlayer.getSeats().indexOf(s);
                        }
                        break;
                    }

                    if (splitInd == -1) {
                        logger.error("splitInd is null");
                        return null;
                    }

                    splitPlayer.getSeats().set(splitInd, seat);

                    //splitPlayer.changeBalance(-seat.getCurrentBet());//balance was changed
                    splitPlayer.changeBalance(seat.getCurrentBet().negate());//balance was changed

                    //need to think over the bet for the split

                    //
                    //
                    //

                }*/ else if (firstGameDecision.equals(GameDecision.CASH_OUT)) {
                    curSeat.setLastGameDecision(firstGameDecision);
                    System.out.println(curSeat.getPlayerId() + " cashed-out");
                    curSeat.setRoundResult(FinalRoundResult.CASH_OUT);
                    SeatResponse seatResponse = SeatMapper.toDto(curSeat);
                    messageSenderImpl.broadcast(new WsMessage<>(seatResponse, WsMessageType.GAME_SEAT_UPDATED));

                    break;
                }

            }
            if (curSeat.getMainScore() == 21 && curSeat.getMainHand().size() == 2) {
                System.out.println(curSeat.getPlayerId() + " has BLACKJACK (" + curSeat.getMainScore() + ") - amazing");

                ///if the dealer has BJ too, in this case the player will have PUSH
                curSeat.setRoundResult(ProgressRoundResult.BLACKJACK);
                SeatResponse seatResponse = SeatMapper.toDto(curSeat);
                messageSenderImpl.broadcast(new WsMessage<>(seatResponse, WsMessageType.GAME_SEAT_UPDATED));

            }
            if (curSeat.getMainScore() == 21 && curSeat.getMainHand().size() > 2) {
                System.out.println(curSeat.getPlayerId() + " has " + curSeat.getMainScore() + " - good catch");
                SeatResponse seatResponse = SeatMapper.toDto(curSeat);
                messageSenderImpl.broadcast(new WsMessage<>(seatResponse, WsMessageType.GAME_SEAT_UPDATED));
                curSeat.setRoundResult(ProgressRoundResult.STAND);

            }
            if (curSeat.getMainScore() > 21) {
                System.out.println(curSeat.getPlayerId() + " has TOO MANY (" + curSeat.getMainScore() + ") - sadly");

                curSeat.setRoundResult(FinalRoundResult.LOSE);
            }
        }

        changeGameStatusForInterface(GamePhaseUI.DEALER_DECISION);

        try {
            Thread.sleep(TIME_BETWEEN_CARDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        dealer.calculateScore(dealer.getHiddenCard());
        messageSenderImpl.broadcast(new WsMessage<>(DealerMapper.toDto(dealer), WsMessageType.DEALER));
        try {
            Thread.sleep(TIME_BETWEEN_CARDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        boolean isEveryOneBust = true;
        for (Seat playerSeat : gameSeats) {
            if (playerSeat.getRoundResult() == FinalRoundResult.LOSE) {
                isEveryOneBust = true;
            } else {
                isEveryOneBust = false;
            }
        }

        ///check dealer's score and then appropriate actions (hit or stand)
        if (!isEveryOneBust) {
            while (dealer.getScore() < 17) {

                System.out.println("hit for 'Dealer'");
                Card card = takeCard();
                dealer.calculateScore(card);
                messageSenderImpl.broadcast(new WsMessage<>(DealerMapper.toDto(dealer), WsMessageType.DEALER));
                System.out.println(card.getInitial() + " of " + card.getSuit() + " was " +
                        "dealt to '" + dealer.getNickName() + "', score - " + dealer.getScore());

                if (dealer.getScore() < 17) {
                    try {
                        Thread.sleep(TIME_BETWEEN_CARDS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } else {
            //TODO mb to do smth
        }

        {///this block of code is for dealer's turn (after playersInGameSession)...
            System.out.println("Game results:");
            changeGameStatusForInterface(GamePhaseUI.RESULT_ANNOUNCEMENT);

            ///check dealer's score - BJ
            if (dealer.getScore() == 21 && dealer.getHand().size() == 2) {
                System.out.println("Unfortunately, " + dealer.getNickName() + " has BLACKJACK (" + dealer.getScore() + ")");

                dealer.setRoundResult(RoundResult.BLACKJACK);
                messageSenderImpl.broadcast(new WsMessage<>(DealerMapper.toDto(dealer), WsMessageType.DEALER));


                gameSeats.stream()//TODO develop insurance if Dealer has ace
                        .filter(p -> p.getRoundResult() == ProgressRoundResult.BLACKJACK)
                        .forEach(p -> p.setRoundResult(FinalRoundResult.PUSH));

                gameSeats.stream()
                        .filter(s -> s.getRoundResult() == ProgressRoundResult.STAND)
                        .forEach(p -> p.setRoundResult(FinalRoundResult.LOSE));
            }

            ///check dealer's score - quells 21 and not BJ
            if (dealer.getScore() == 21 && dealer.getHand().size() > 2) {
                System.out.println(dealer.getNickName() + " has " + dealer.getScore());

                gameSeats.stream()
                        .filter(p -> p.getMainScore() == 21 &&
                                p.getRoundResult() == ProgressRoundResult.STAND)
                        .forEach(p -> p.setRoundResult(FinalRoundResult.PUSH));

                gameSeats.stream()
                        .filter(p -> p.getRoundResult() == ProgressRoundResult.STAND)
                        .forEach(p -> p.setRoundResult(FinalRoundResult.LOSE));
            }

            ///check dealer's score - less or equals 20
            if (dealer.getScore() <= 20) {
                System.out.println(dealer.getNickName() + " has " + dealer.getScore());

                gameSeats.stream()
                        .filter(p -> p.getMainScore() == dealer.getScore() &&
                                p.getRoundResult() == ProgressRoundResult.STAND)//отметка, что он к примеру не кєшанул
                        //или тп, а ещё в игре
                        .forEach(p -> p.setRoundResult(FinalRoundResult.PUSH));

                gameSeats.stream()
                        .filter(s -> s.getMainScore() < dealer.getScore() &&
                                s.getRoundResult() == ProgressRoundResult.STAND)
                        .forEach(p -> p.setRoundResult(FinalRoundResult.LOSE));

                gameSeats.stream()
                        .filter(p -> p.getMainScore() > dealer.getScore() &&
                                p.getRoundResult() == ProgressRoundResult.STAND)
                        .forEach(p -> p.setRoundResult(FinalRoundResult.WIN));
            }

            ///check dealer's score - more than 21 (too many)
            if (dealer.getScore() > 21) {
                System.out.println(dealer.getNickName() + " has TOO MANY (" + dealer.getScore() + ")");

                dealer.setRoundResult(RoundResult.BUST);
                messageSenderImpl.broadcast(new WsMessage<>(DealerMapper.toDto(dealer), WsMessageType.DEALER));

                gameSeats.stream()
                        .filter(p -> p.getMainScore() <= 21 &&
                                p.getRoundResult() == ProgressRoundResult.STAND)
                        .forEach(p -> p.setRoundResult(FinalRoundResult.WIN));
            }
        }

        //TODO implement separate GAME logger
        ///output the dealer's game results to the console
        if (dealer.getRoundResult() == RoundResult.BLACKJACK ||
                dealer.getRoundResult() == RoundResult.BUST) {//TODO что-то я не понял почему тут BJ и TM в однои if-е...

            System.out.println(dealer.getNickName() + " has " + dealer.getRoundResult());

        } else {
            System.out.println(dealer.getNickName() + " has " + dealer.getScore());
        }

        ///output the seats' game results to the console
        gameSeats.forEach(s -> System.out.println("Player on seat" + s.getSeatNumber() + " - " + s.getRoundResult()));

        for (Seat s : gameSeats) {
            if (s.getRoundResult() == null)
                logger.error("Some seat has no round result");
        }

        distributeMoney();

        //TODO change GAME_RESULT -> send smth like SeatResultDto instead of gameSeats...
        List<SeatResponse> gameSeatsDto = SeatMapper.toDtoList(gameSeats);
        messageSenderImpl.broadcast(new WsMessage<>(gameSeatsDto, WsMessageType.GAME_RESULT));///broadcasting of gameSeats with last game data

        ///this block is for checking amount of cards (for shuffle)
        if (gameDeck.size() < ((gameSeats.size() + 1) * 4)) {
            System.err.println("There are few cards left in the shoe...");
            gameDeck = smallDeckObject.getSmallDeck();
            Shuffler.shuffle(gameDeck);
        }

        ///delay after RESULT_ANNOUNCEMENT to give players time to see game results
        try {
            Thread.sleep(TIME_FOR_RESULT_ANNOUNCEMENT);//5k
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<PlayerSnapshot> playerSnapshots = gamePlayers.stream()
                .filter(Player::isInTheGame)
                .map(player -> new PlayerSnapshot(
                        player.getUserId(),
                        player.getNickname(),
                        player.getGameProfit(),
                        player.getSeats().stream()
                                .filter(Seat::isInTheGame)
                                .map(seat -> new SeatSnapshot(
                                        player.getUserId(),
                                        seat.getSeatNumber(),
                                        seat.getMainScore(),
                                        new ArrayList<>(seat.getMainHand()),
                                        seat.getCurrentBet(),
                                        seat.getLastGameDecision(),
                                        seat.getRoundResult()
                                ))
                                .toList(),
                        player.isInTheGame()
                ))
                .toList();

        GameResultSnapshot gameResultSnapshot = new GameResultSnapshot(playerSnapshots, dealer.getScore());

        clearPlayersAndSeatsAfterGame();

        playersBroadcast();///need because of s.restartAfterGame() for every player

        dealer.fullSeatReset();

        for (Player player : gamePlayers) {
            if (player.getSeats().isEmpty()) {
                messageSenderImpl.sendToClient(player.getUserId(), new WsMessage<>(GamePhaseUI.EMPTY_TABLE, WsMessageType.E_GAME_STATUS_FOR_INTERFACE));
            } else {
                messageSenderImpl.sendToClient(player.getUserId(), new WsMessage<>(GamePhaseUI.PLACING_BETS, WsMessageType.E_GAME_STATUS_FOR_INTERFACE));
            }
        }

        clearGameDataAfterGame();

        table.setDealer(null);

        //TODO mb combine GAME_RESULT and GAME_FINISHED somehow...
        List<SeatResponse> seatsDto = SeatMapper.toDtoList(table.getSeats());
        messageSenderImpl.broadcast(new WsMessage<>(seatsDto, WsMessageType.GAME_FINISHED));//sending exactly busy seats at the table (not gameSeats)...
        //TODO it is necessary to make it so that the player cannot take more seats until the game is over

        //TODO (regarding the top one, I don't know how it works) so far in this implementation it's done so that when GAME _ FINISHED, a collection of regular places is sent
        //TODO so that players (and spectators) receive an up-to-date list of places

        table.setGame(false);
        isGameRunning = false;

        return gameResultSnapshot;
    }

    private boolean isValidNextDecision(GameDecision gameDecision) {
        return gameDecision.equals(GameDecision.HIT) ||
                gameDecision.equals(GameDecision.CASH_OUT) ||
                gameDecision.equals(GameDecision.STAND);
    }

    public void distributeMoney() {

        if (gamePlayers == null)
            throw new BlackjackException("Player collection is null in game during money distributing.");

        IRoundResult result;
        for (Seat seat : gameSeats) {
            result = seat.getRoundResult();

            Player curPlayer = null;
            for (Player player : gamePlayers) {
                if (player.getUserId().equals(seat.getPlayerId())) {
                    curPlayer = player;
                }
            }

            if (curPlayer == null)
                throw new BlackjackException("Player not found in game during money distributing.");

            if (result == FinalRoundResult.CASH_OUT) {
                curPlayer.changeGameProfit(seat.getCurrentBet().divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP));
            }

            if (result == FinalRoundResult.WIN) {
                curPlayer.changeGameProfit(seat.getCurrentBet().multiply(BigDecimal.valueOf(2)));

            }

            if (result == FinalRoundResult.BLACKJACK) {
                curPlayer.changeGameProfit(seat.getCurrentBet().multiply(BigDecimal.valueOf(2.5)));
            }

            if (result == FinalRoundResult.PUSH) {
                curPlayer.changeGameProfit(seat.getCurrentBet());
            }
        }

        playersBroadcast();

        for (Player player : gamePlayers) {
            System.err.println(player.getUserId() + " - gameProfit: " + player.getGameProfit());
        }
    }

    public GameDecision gettingDecision(Seat seat) {
        messageSenderImpl.broadcast(new WsMessage<>(SeatMapper.toDto(seat), WsMessageType.CURRENT_SEAT));

        table.setTurnOfSeat(seat);

        timerService.start(TimerType.DECISION_TIME, decisionTimeObserver);

        try {///it's necessarily because of threads...
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        while (gameDecisionField == null && timerService.isRunning(TimerType.DECISION_TIME)) {
            System.out.println("Decision button is empty, but timer is running yet");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (gameDecisionField != null) {
            System.out.println("Decision was made on time");
            return getGameDecisionField();
        }

        if (gameDecisionField == null) {
            GameDecision gameDecision = basicDecision(seat);
            logger.info("decisionField == null and timer is over -> basicDecision - " + gameDecision);
            return gameDecision;
        }

        return null;
    }

    public GameDecision basicDecision(Seat seat) {
        if (seat.getMainScore() > 11) {
            seat.setRoundResult(ProgressRoundResult.STAND);
            return GameDecision.STAND;
        } else {
            return GameDecision.HIT;
        }
    }

    public boolean isAbleToSplit(Seat seat) {//idk
        return seat.getMainHand().getFirst().getCoefficient() ==
                seat.getMainHand().getLast().getCoefficient();
    }

    public boolean isFirstDecision(Seat seat) {//idk
        return seat.getLastGameDecision() == null;
    }

    private Card takeCard() {
        return gameDeck.removeLast();
    }

    public void playersBroadcast() {///It's for sending to certain player his player data
        for (Player player : gamePlayers) {
            messageSenderImpl.sendToClient(player.getUserId(), new WsMessage<>(PlayerMapper.toResponse(player), WsMessageType.PLAYER_DATA));
        }
    }

    private void preparePlayersAndSeatsForGame() {
        gameSeats = table.getAndSetGameSeats();
        gamePlayers = table.markAndGetPlayersInGame();

        restartPlayerAndSeatInfoBeforeGame();
    }

    private void clearPlayersAndSeatsAfterGame() {
        restartPlayerAndSeatInfoAfterGame();
    }

    private void clearGameDataAfterGame() {
        gameSeats = null;
        gamePlayers = null;
    }

    private void restartPlayerAndSeatInfoBeforeGame() {
        gameDecisionField = null;

        gamePlayers.stream()
                .filter(Player::isInTheGame)
                .forEach(Player::restartBeforeGame);
    }

    private void restartPlayerAndSeatInfoAfterGame() {
        gameDecisionField = null;

        gamePlayers.stream()
                .filter(Player::isInTheGame)
                .forEach(Player::restartAfterGame);
    }
}