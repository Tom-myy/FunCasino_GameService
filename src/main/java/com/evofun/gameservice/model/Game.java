package com.evofun.gameservice.model;

import com.evofun.gameservice.MoneyServiceClient;
import com.evofun.gameservice.common.error.ErrorCode;
import com.evofun.gameservice.common.error.ErrorDto;
import com.evofun.gameservice.db.GameResultSnapshot;
import com.evofun.gameservice.db.PlayerSnapshot;
import com.evofun.gameservice.db.SeatSnapshot;
import com.evofun.gameservice.game.*;
import com.evofun.gameservice.mapper.DealerMapper;
import com.evofun.gameservice.mapper.PlayerPublicMapper;
import com.evofun.gameservice.mapper.SeatMapper;
import com.evofun.gameservice.mapper.TableMapper;
import com.evofun.gameservice.game.timer.DecisionTimeObserver;
import com.evofun.gameservice.game.timer.TimerService;
import com.evofun.gameservice.game.timer.TimerType;
import com.evofun.gameservice.dto.SeatDto;
import com.evofun.gameservice.websocket.message.WsMessage;
import com.evofun.gameservice.websocket.message.WsMessageSenderImpl;
import com.evofun.gameservice.websocket.message.WsMessageType;
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

public class Game {
    private static final Logger logger = LoggerFactory.getLogger(Game.class);
    private static final int TIME_FOR_DECISION = 1000;
    private static final int TIME_FOR_RESULT_ANNOUNCEMENT = 5000;
    private static final int TIME_BETWEEN_CARDS = 1000;
    private DeckModel deckModelObject = new DeckModel();
    private List<CardModel> gameDeck = null;
    private DealerModel dealerModel;
    private TableModel tableModel;
    private TimerService timerService;
    private PlayerRegistry playerRegistry;
    @Getter
    @Setter
    private boolean isGameRunning = false;
    private DecisionTimeObserver decisionTimeObserver;

    private final WsMessageSenderImpl messageSenderImpl;//TODO bad violation!

    private final MoneyServiceClient moneyServiceClient;//TODO move to other layer coz of layer violation!!!

    private List<PlayerModel> gamePlayers = null;//for money management

    private GamePhaseUI gameStatusForInterface = GamePhaseUI.EMPTY_TABLE;

    private static final int COUNT_OF_INITIAL_CARDS = 2;
    private static final int MINIMUM_ACE_SUMMAND = 1;
    private static final int MAXIMUM_ACE_SUMMAND = 11;


    private List<SeatModel> gameSeatModels;

    private GameDecision gameDecisionField = null;

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

    public Game(TableModel tableModel, WsMessageSenderImpl messageSenderImpl, TimerService timerService, PlayerRegistry playerRegistry, DecisionTimeObserver decisionTimeObserver, MoneyServiceClient moneyServiceClient) {
        this.tableModel = tableModel;
        this.messageSenderImpl = messageSenderImpl;
        this.timerService = timerService;
        this.playerRegistry = playerRegistry;
        this.decisionTimeObserver = decisionTimeObserver;
        gamePlayers = playerRegistry.getPlayerModels();
        this.moneyServiceClient = moneyServiceClient;
    }

    public GameResultSnapshot startGame() {
        if (tableModel.isGame()) {
            return null;
        } else {
            tableModel.setGame(true);
            isGameRunning = true;
        }

        timerService.stop(TimerType.BETTING_TIME);

        preparePlayersAndSeatsForGame();
        for(PlayerModel player: gamePlayers) {
            messageSenderImpl.sendToClient(
                    player.getUserId(),
                    new WsMessage<>(PlayerPublicMapper.toPlayerPublicDto(player), WsMessageType.PLAYER_DATA)
            );
        }

        gameSeatModels = gameSeatModels.stream()
                .sorted(Comparator.comparingInt(SeatModel::getSeatNumber))
                .collect(Collectors.toList());

        for (SeatModel s : gameSeatModels) {//TODO delete
            s.printMoneyInfo();
        }

        tableModel.setDealerModel(new DealerModel());
        dealerModel = tableModel.getDealerModel();
        messageSenderImpl.broadcast(new WsMessage<>(TableMapper.toDto(tableModel), WsMessageType.GAME_STARTED));//mb send after resetGameResultStatus
        messageSenderImpl.broadcast(new WsMessage<>(DealerMapper.toDto(dealerModel), WsMessageType.DEALER));//TODO mb not to send the dealer (//mb send after resetGameResultStatus)

        if (gameDeck == null) {
            gameDeck = new ArrayList<>(deckModelObject.getOneUsualDeck());//TODO mb change smth here
            Collections.shuffle(gameDeck);
        }

        for (SeatModel seatModel : gameSeatModels) { //TODO mb change PROGRESSING to null
            seatModel.resetGameResultStatus();
        }
        messageSenderImpl.broadcast(new WsMessage<>(RoundResult.PROGRESSING, WsMessageType.E_GAME_RESULT_STATUS));

        System.out.println("Bets are closed, good luck!");

        changeGameStatusForInterface(GamePhaseUI.DEALING_CARDS);//TODO mb i dont need it

        System.out.println("\nInitial cards:");
        for (int i = 1; i <= COUNT_OF_INITIAL_CARDS; ++i) {
            for (SeatModel seatModel : gameSeatModels) {

                CardModel cardModel = takeCard();

                seatModel.calculateScore(cardModel);

                if (seatModel.getMainScore() == 21) {
                    System.out.println(cardModel.getInitial() + " of " + cardModel.getSuit() + " was " +
                            "dealt to player on seat #" + seatModel.getSeatNumber() + ", score - BLACKJACK (" + seatModel.getMainScore() + ")");
                    //TODO display it in the playersInGameSession' interface

                    seatModel.setRoundResult(ProgressRoundResult.BLACKJACK);
                    SeatDto seatDto = SeatMapper.toDto(seatModel);
                    messageSenderImpl.broadcast(new WsMessage<>(seatDto, WsMessageType.GAME_SEAT_UPDATED));

                } else {
                    System.out.println(cardModel.getInitial() + " of " + cardModel.getSuit() + " was " +
                            "dealt to player on seat #" + seatModel.getSeatNumber() + ", score = " + seatModel.getMainScore());
                    SeatDto seatDto = SeatMapper.toDto(seatModel);
                    messageSenderImpl.broadcast(new WsMessage<>(seatDto, WsMessageType.GAME_SEAT_UPDATED));
                }

                try {
                    Thread.sleep(TIME_BETWEEN_CARDS);//here was 1s
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            CardModel cardModel = takeCard();

            if (i == 1) {
                dealerModel.calculateScore(cardModel);
                System.out.println(cardModel.getInitial() + " of " + cardModel.getSuit() + " was " +
                        "dealt to '" + dealerModel.getNickName() + "', score = " + dealerModel.getScore());

                messageSenderImpl.broadcast(new WsMessage<>(DealerMapper.toDto(dealerModel), WsMessageType.DEALER));

            } else {//TODO do it more beautiful and smarter
                System.out.println("hidden card was dealt to '" + dealerModel.getNickName() + "'" +
                        ", score = " + dealerModel.getCurrentCardInHandByIndex(0).getCoefficient() + "+");

                dealerModel.setHiddenCardModel(cardModel);
                messageSenderImpl.broadcast(new WsMessage<>(DealerMapper.toDto(dealerModel), WsMessageType.DEALER));
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

        for (SeatModel curSeat : gameSeatModels) {

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
                    SeatDto seatDto = SeatMapper.toDto(curSeat);
                    messageSenderImpl.broadcast(new WsMessage<>(seatDto, WsMessageType.GAME_SEAT_UPDATED));
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
                        CardModel cardModel = takeCard();

                        curSeat.calculateScore(cardModel);

                        System.out.println(cardModel.getInitial() + " of " + cardModel.getSuit() + " was " +
                                "dealt to '" + curSeat.getPlayerId() + "', score = " + curSeat.getMainScore());

                        SeatDto seatDto = SeatMapper.toDto(curSeat);
                        messageSenderImpl.broadcast(new WsMessage<>(seatDto, WsMessageType.GAME_SEAT_UPDATED));

                        if (curSeat.getMainScore() < 21) {
                            do {
                                System.out.print(curSeat.getPlayerId() + " has " + curSeat.getMainScore() + ", what is your next decision? (hit, cash-out, stand) - ");//todo сделать, чтобы это предлагал дилер

                                nextGameDecision = gettingDecision(curSeat);
                                //TODO по идее нужна проверка на налл как и с первым решением
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
                                    seatDto = SeatMapper.toDto(curSeat);
                                    messageSenderImpl.broadcast(new WsMessage<>(seatDto, WsMessageType.GAME_SEAT_UPDATED));

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
                    PlayerModel curPlayer = null;
                    for (PlayerModel player : gamePlayers) {
                        if (player.getUserId().equals(curSeat.getPlayerId())) {
                            curPlayer = player;
                        }
                    }

                    if (curPlayer == null) {
                        logger.error("curPlayer is null");
                        return null;
                    }

                    SeatModel tmpSeatModel = null;
                    int tmpInd = -1;
                    for (SeatModel s : curPlayer.getSeatModels()) {
                        if (curSeat.getSeatNumber() == s.getSeatNumber()) {
                            tmpSeatModel = s;
                            tmpInd = curPlayer.getSeatModels().indexOf(s);
                        }
                    }

                    if (tmpSeatModel == null || tmpInd == -1) {
                        logger.error("tmpSeat or tmpInd is wrong");
                        return null;
                    }

                    curPlayer.getSeatModels().set(tmpInd, curSeat);

//       TODO - MB don't need!!!              playersBroadcast();//TODO think here, coz in fact i dont need broadcast (i change only one Player)

                    curSeat.setLastGameDecision(firstGameDecision);
                    System.out.println(curSeat.getPlayerId() + " decided to " + firstGameDecision);

                    CardModel cardModel = takeCard();

                    curSeat.calculateScore(cardModel);

                    SeatDto seatDto = SeatMapper.toDto(curSeat);
                    messageSenderImpl.broadcast(new WsMessage<>(seatDto, WsMessageType.GAME_SEAT_UPDATED));

                    System.out.println(firstGameDecision + " for " + curSeat.getPlayerId());
                    System.out.println(cardModel.getInitial() + " of " + cardModel.getSuit() + " was " +
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

                    //TODO finish this split option
                    //TODO finish this split option

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

//                    splitPlayer.changeBalance(-seat.getCurrentBet());//balance was changed
                    splitPlayer.changeBalance(seat.getCurrentBet().negate());//balance was changed

                    //need to think over the bet for the split and need to send this player

                    //
                    //
                    //

                }*/ else if (firstGameDecision.equals(GameDecision.CASH_OUT)) {
                    curSeat.setLastGameDecision(firstGameDecision);
                    System.out.println(curSeat.getPlayerId() + " cashed-out");
                    curSeat.setRoundResult(FinalRoundResult.CASH_OUT);
                    SeatDto seatDto = SeatMapper.toDto(curSeat);
                    messageSenderImpl.broadcast(new WsMessage<>(seatDto, WsMessageType.GAME_SEAT_UPDATED));

                    break;
                }

            }
            if (curSeat.getMainScore() == 21 && curSeat.getMainHand().size() == 2) {
                System.out.println(curSeat.getPlayerId() + " has BLACKJACK (" + curSeat.getMainScore() + ") - amazing");

                curSeat.setRoundResult(ProgressRoundResult.BLACKJACK);//тк если у диллера тоже BJ, то у игрока PUSH
                SeatDto seatDto = SeatMapper.toDto(curSeat);
                messageSenderImpl.broadcast(new WsMessage<>(seatDto, WsMessageType.GAME_SEAT_UPDATED));

            }
            if (curSeat.getMainScore() == 21 && curSeat.getMainHand().size() > 2) {
                System.out.println(curSeat.getPlayerId() + " has " + curSeat.getMainScore() + " - good catch");
                SeatDto seatDto = SeatMapper.toDto(curSeat);
                messageSenderImpl.broadcast(new WsMessage<>(seatDto, WsMessageType.GAME_SEAT_UPDATED));
                curSeat.setRoundResult(ProgressRoundResult.STAND);

            }
            if (curSeat.getMainScore() > 21) {
                System.out.println(curSeat.getPlayerId() + " has TOO MANY (" + curSeat.getMainScore() + ") - sadly");

                curSeat.setRoundResult(FinalRoundResult.LOSE);//как по мне - не особо правильно это тут распологать
            }
        }

        changeGameStatusForInterface(GamePhaseUI.DEALER_DECISION);

        try {
            Thread.sleep(TIME_BETWEEN_CARDS);//1.5s but it's not TIME_BETWEEN_CARDS
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        dealerModel.calculateScore(dealerModel.getHiddenCardModel());
        messageSenderImpl.broadcast(new WsMessage<>(DealerMapper.toDto(dealerModel), WsMessageType.DEALER));//TODO is this necessary?
        try {
            Thread.sleep(TIME_BETWEEN_CARDS);//1s but it's not TIME_BETWEEN_CARDS
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        boolean isEveryOneBust = true;
        for (SeatModel playerSeat : gameSeatModels) {//mb to think over
            if (playerSeat.getRoundResult() == FinalRoundResult.LOSE) {
                isEveryOneBust = true;
            } else {
                isEveryOneBust = false;
            }
        }

        //check dealer's score and then appropriate actions (hit or stand)
        if (!isEveryOneBust) {
            while (dealerModel.getScore() < 17) {

                System.out.println("hit for 'Dealer'");
                CardModel cardModel = takeCard();
                dealerModel.calculateScore(cardModel);
                messageSenderImpl.broadcast(new WsMessage<>(DealerMapper.toDto(dealerModel), WsMessageType.DEALER));
                System.out.println(cardModel.getInitial() + " of " + cardModel.getSuit() + " was " +
                        "dealt to '" + dealerModel.getNickName() + "', score - " + dealerModel.getScore());

                if (dealerModel.getScore() < 17) {
                    try {
                        Thread.sleep(TIME_BETWEEN_CARDS);//1s but it's not TIME_BETWEEN_CARDS
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } else {
            //TODO mb to do smth
        }

        {//this block of code is for dealer's turn (after playersInGameSession)...
            System.out.println("Game results:");
            changeGameStatusForInterface(GamePhaseUI.RESULT_ANNOUNCEMENT);

            //check dealer's score - BJ
            if (dealerModel.getScore() == 21 && dealerModel.getHand().size() == 2) {
                System.out.println("Unfortunately, " + dealerModel.getNickName() + " has BLACKJACK (" + dealerModel.getScore() + ")");

                dealerModel.setRoundResult(RoundResult.BLACKJACK);
                messageSenderImpl.broadcast(new WsMessage<>(DealerMapper.toDto(dealerModel), WsMessageType.DEALER));


                gameSeatModels.stream()//TODO develop insurance if Dealer has ace
                        .filter(p -> p.getRoundResult() == ProgressRoundResult.BLACKJACK)
                        .forEach(p -> p.setRoundResult(FinalRoundResult.PUSH));

                gameSeatModels.stream()
                        .filter(s -> s.getRoundResult() == ProgressRoundResult.STAND)
                        .forEach(p -> p.setRoundResult(FinalRoundResult.LOSE));
            }

            //check dealer's score - quells 21 and not BJ
            if (dealerModel.getScore() == 21 && dealerModel.getHand().size() > 2) {
                System.out.println(dealerModel.getNickName() + " has " + dealerModel.getScore());

                gameSeatModels.stream()
                        .filter(p -> p.getMainScore() == 21 &&
                                p.getRoundResult() == ProgressRoundResult.STAND)
                        .forEach(p -> p.setRoundResult(FinalRoundResult.PUSH));

                gameSeatModels.stream()
                        .filter(p -> p.getRoundResult() == ProgressRoundResult.STAND)
                        .forEach(p -> p.setRoundResult(FinalRoundResult.LOSE));
            }

            //check dealer's score - less or equals 20
            if (dealerModel.getScore() <= 20) {
                System.out.println(dealerModel.getNickName() + " has " + dealerModel.getScore());

                gameSeatModels.stream()
                        .filter(p -> p.getMainScore() == dealerModel.getScore() &&
                                p.getRoundResult() == ProgressRoundResult.STAND)//отметка, что он к примеру не кєшанул
                        //или тп, а ещё в игре
                        .forEach(p -> p.setRoundResult(FinalRoundResult.PUSH));

                gameSeatModels.stream()
                        .filter(s -> s.getMainScore() < dealerModel.getScore() &&
                                s.getRoundResult() == ProgressRoundResult.STAND)
                        .forEach(p -> p.setRoundResult(FinalRoundResult.LOSE));

                gameSeatModels.stream()
                        .filter(p -> p.getMainScore() > dealerModel.getScore() &&
                                p.getRoundResult() == ProgressRoundResult.STAND)
                        .forEach(p -> p.setRoundResult(FinalRoundResult.WIN));
            }

            //check dealer's score - more than 21 (too many)
            if (dealerModel.getScore() > 21) {
                System.out.println(dealerModel.getNickName() + " has TOO MANY (" + dealerModel.getScore() + ")");

                dealerModel.setRoundResult(RoundResult.BUST);
                messageSenderImpl.broadcast(new WsMessage<>(DealerMapper.toDto(dealerModel), WsMessageType.DEALER));

                gameSeatModels.stream()
                        .filter(p -> p.getMainScore() <= 21 &&
                                p.getRoundResult() == ProgressRoundResult.STAND)
                        .forEach(p -> p.setRoundResult(FinalRoundResult.WIN));
            }
        }

        //output the dealer's game results to the console
        if (dealerModel.getRoundResult() == RoundResult.BLACKJACK ||
                dealerModel.getRoundResult() == RoundResult.BUST) {//TODO что-то я не понял почему тут BJ и TM в однои if-е...

            System.out.println(dealerModel.getNickName() + " has " + dealerModel.getRoundResult());

        } else {
            System.out.println(dealerModel.getNickName() + " has " + dealerModel.getScore());
        }

        //output the seats' game results to the console
        gameSeatModels.forEach(s -> System.out.println("Player on seat" + s.getSeatNumber() + " - " + s.getRoundResult()));

        for(SeatModel s: gameSeatModels) {
            if(s.getRoundResult() == null)
                logger.error("Some seat has no round result");
        }

        distributeMoney();

        //TODO change GAME_RESULT -> send smth like SeatResultDto instead of gameSeats...
        List<SeatDto> gameSeatsDto = SeatMapper.toDtoList(gameSeatModels);
        messageSenderImpl.broadcast(new WsMessage<>(gameSeatsDto, WsMessageType.GAME_RESULT));//broadcasting of gameSeats with last game data

        //this block is for checking amount of cards
        if (gameDeck.size() < ((gameSeatModels.size() + 1) * 4)) {
            System.err.println("There are few cards left in the shoe...");
            gameDeck = deckModelObject.getOneUsualDeck();
            DeckShufflerModel.myShuffle(gameDeck);
        }

        //delay after RESULT_ANNOUNCEMENT to give playersInGameSession time to see game results
        try {
            Thread.sleep(TIME_FOR_RESULT_ANNOUNCEMENT);//5k
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<PlayerSnapshot> playerSnapshots = gamePlayers.stream()
                .filter(PlayerModel::isInTheGame)
                .map(player -> new PlayerSnapshot(
                        player.getUserId(),
                        player.getNickname(),
                        player.getGameProfit(),
                        player.getSeatModels().stream()
                                .filter(SeatModel::isInTheGame)
                                .map(seat -> new SeatSnapshot(
                                        player.getUserId(),
                                        seat.getSeatNumber(),
                                        seat.getMainScore(),
                                        new ArrayList<>(seat.getMainHand()), // если список может меняться — создаём копию
                                        seat.getCurrentBet(),
                                        seat.getLastGameDecision(),
                                        seat.getRoundResult()
                                ))
                                .toList(),
                        player.isInTheGame()
                ))
                .toList();

        GameResultSnapshot gameResultSnapshot = new GameResultSnapshot(playerSnapshots, dealerModel.getScore());

        clearPlayersAndSeatsAfterGame();

        playersBroadcast();//need because of s.restartAfterGame() for every player

        //reset dealer's game data
        dealerModel.fullSeatReset();


        for (PlayerModel player : gamePlayers) {
            if (player.getSeatModels().isEmpty()) {
                messageSenderImpl.sendToClient(player.getUserId(), new WsMessage<>(GamePhaseUI.EMPTY_TABLE, WsMessageType.E_GAME_STATUS_FOR_INTERFACE));
            } else {
                messageSenderImpl.sendToClient(player.getUserId(), new WsMessage<>(GamePhaseUI.PLACING_BETS, WsMessageType.E_GAME_STATUS_FOR_INTERFACE));
            }
        }

        clearGameDataAfterGame();

        tableModel.setDealerModel(null);//мб необязательно

        //TODO mb combine GAME_RESULT and GAME_FINISHED somehow...
        List<SeatDto> seatsDto = SeatMapper.toDtoList(tableModel.getSeatModels());
        messageSenderImpl.broadcast(new WsMessage<>(seatsDto, WsMessageType.GAME_FINISHED));//sending exactly busy seats at the table (not gameSeats)...
        //TODO надо сделать так, чтобы играющий не мог занять ещё места пока не закончится игра

        //TODO (по поводу верхнего я хз как оно) пока что в этой реализации сделано так, что когда GAME _ FINISHED, то отправляется коллекция обычных мест
        //TODO чтобы играющие (и наблюдающие за игрой) получили актуальный список мест

        tableModel.setGame(false);
        isGameRunning = false;

        return gameResultSnapshot;
    }

    private boolean isValidNextDecision(GameDecision gameDecision) {
        return gameDecision.equals(GameDecision.HIT) ||
                gameDecision.equals(GameDecision.CASH_OUT) ||
                gameDecision.equals(GameDecision.STAND);
    }

    public void distributeMoney() {//TODO check if everything works properly

        if (gamePlayers == null)
            throw new BlackjackException("Player collection is null in game during money distributing.");

        IRoundResult result;
        for (SeatModel seatModel : gameSeatModels) {
            result = seatModel.getRoundResult();

            PlayerModel curPlayer = null;
            for (PlayerModel player : gamePlayers) {
                if (player.getUserId().equals(seatModel.getPlayerId())) {
                    curPlayer = player;
                }
            }

            if (curPlayer == null)
                throw new BlackjackException("Player not found in game during money distributing.");

            if (result == FinalRoundResult.CASH_OUT) {
                curPlayer.changeGameProfit(seatModel.getCurrentBet().divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP));
            }

            if (result == FinalRoundResult.WIN) {
                curPlayer.changeGameProfit(seatModel.getCurrentBet().multiply(BigDecimal.valueOf(2)));

            }

            if (result == FinalRoundResult.BLACKJACK) {
                curPlayer.changeGameProfit(seatModel.getCurrentBet().multiply(BigDecimal.valueOf(2.5)));
            }

            if (result == FinalRoundResult.PUSH) {
                curPlayer.changeGameProfit(seatModel.getCurrentBet());
            }
        }

        playersBroadcast();//if im not wrong - its for sending of results at the end of the game

        for (PlayerModel player : gamePlayers) {
            System.err.println(player.getUserId() + " - gameProfit: " + player.getGameProfit());
        }
    }

    public GameDecision gettingDecision(SeatModel seatModel) {
        //TODO display it in the playersInGameSession' interface
        messageSenderImpl.broadcast(new WsMessage<>(SeatMapper.toDto(seatModel), WsMessageType.CURRENT_SEAT));

        tableModel.setTurnOfSeat(seatModel);

        timerService.start(TimerType.DECISION_TIME, /*TIME_FOR_DECISION,*/ decisionTimeObserver);

        try {//it's necessarily because of thread...
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
            GameDecision gameDecision = basicDecision(seatModel);
            logger.info("decisionField == null and timer is over -> basicDecision - " + gameDecision);
            return gameDecision;
        }

        return null;
    }

    public GameDecision basicDecision(SeatModel seatModel) {
        if (seatModel.getMainScore() > 11) {
            seatModel.setRoundResult(ProgressRoundResult.STAND);
            return GameDecision.STAND;
        } else {
            return GameDecision.HIT;
        }
    }

    public boolean isAbleToSplit(SeatModel seatModel) {//idk
        return seatModel.getMainHand().getFirst().getCoefficient() ==
                seatModel.getMainHand().getLast().getCoefficient();
    }

    public boolean isFirstDecision(SeatModel seatModel) {//idk
        return seatModel.getLastGameDecision() == null;
    }

    private CardModel takeCard() {
        System.err.println("-----------------" + gameDeck.size());
        return gameDeck.removeLast();
    }

    //TODO playersBroadcast - bed violation!!!
    public void playersBroadcast() {//It's for sending to certain player his player data
        for (PlayerModel playerModel : gamePlayers) {
            messageSenderImpl.sendToClient(playerModel.getUserId(), new WsMessage<>(PlayerPublicMapper.toPlayerPublicDto(playerModel), WsMessageType.PLAYER_DATA));
        }
    }

    private void preparePlayersAndSeatsForGame(){
        gameSeatModels = tableModel.getAndSetGameSeats();
        gamePlayers = tableModel.markAndGetPlayersInGame();

        restartPlayerAndSeatInfoBeforeGame();
    }

    private void clearPlayersAndSeatsAfterGame() {
        restartPlayerAndSeatInfoAfterGame();
    }

    private void clearGameDataAfterGame() {
        gameSeatModels = null;
        gamePlayers = null;
    }

    private void restartPlayerAndSeatInfoBeforeGame() {
        gameDecisionField = null;

        gamePlayers.stream()
                .filter(PlayerModel::isInTheGame)
                .forEach(PlayerModel::restartBeforeGame);
    }

    private void restartPlayerAndSeatInfoAfterGame() {
        gameDecisionField = null;

        gamePlayers.stream()
                .filter(PlayerModel::isInTheGame)
                .forEach(PlayerModel::restartAfterGame);
    }
}