package com.evofun.gameservice.model;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Game {
    private static final Logger logger = LoggerFactory.getLogger(Game.class);
    private static final int TIME_FOR_DECISION = 1000;
    private static final int TIME_FOR_RESULT_ANNOUNCEMENT = 5000;
    private static final int TIME_BETWEEN_CARDS = 1000;
    private DeckModel deckModelObject = new DeckModel();
    private List<CardModel> gameDeck = null;
    private DealerModel dealerModel;
    private TableModel tableModel;
//    private MyTimer timer;
    private TimerService timerService;
    private PlayerRegistry playerRegistry;
    @Getter
    @Setter
    private boolean isGameRunning = false;
    private DecisionTimeObserver decisionTimeObserver;


/*    PlayersBroadcastCallback playersBroadcastCallback;//It's for sending to certain player his player data
    private GameToMessageHandlerListener listener;*/

    private final WsMessageSenderImpl messageSenderImpl;//TODO bad violation!

    private List<PlayerModel> players;//for money management


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
//            timer.stopTimer();
            timerService.stop(TimerType.DECISION_TIME);
        } else {
            System.err.println("Server got invalid decision: " + gameDecision);
        }
    }

    public void changeGameStatusForInterface(GamePhaseUI status) {
        gameStatusForInterface = status;
//        listener.broadcast(new MyPackage<>(gameStatusForInterface, EMessageType.E_GAME_STATUS_FOR_INTERFACE));
        messageSenderImpl.broadcast(new WsMessage<>(gameStatusForInterface, WsMessageType.E_GAME_STATUS_FOR_INTERFACE));
    }

    public int getCountOfPlayersReadyForGame() {
        return gameSeatModels.size();
    }

    public Game(TableModel tableModel, WsMessageSenderImpl messageSenderImpl, TimerService timerService, PlayerRegistry playerRegistry, DecisionTimeObserver decisionTimeObserver) {
//        this.listener = listener;
        this.tableModel = tableModel;
//        this.playersInGameSession = playersInGameSession;
//        this.playersBroadcastCallback = callback;
//        this.timer = timer;
//        this.messageSender = messageSender;
        this.messageSenderImpl = messageSenderImpl;
        this.timerService = timerService;
        this.playerRegistry = playerRegistry;
        this.decisionTimeObserver = decisionTimeObserver;
        players = playerRegistry.getPlayerModels();
    }

//    public List<PlayerModel> startGame() {
    public GameResultSnapshot startGame() {
        if (tableModel.isGame()) {
            return null;
        } else {
            tableModel.setGame(true);
            isGameRunning = true;
        }

//        timer.stopTimer();
        timerService.stop(TimerType.BETTING_TIME);


//        listener.broadcast(new MyPackage<>("", EMessageType.GAME_STARTED));
        gameSeatModels = tableModel.getAndSetGameSeats();
        for (SeatModel s : gameSeatModels) {//TODO delete
            s.printMoneyInfo();
        }

        for (SeatModel s : gameSeatModels) {
            for (PlayerModel p : players) {
                if (p.getPlayerUUID().equals(s.getPlayerUUID())) {
                    p.setInTheGame(true);

                    messageSenderImpl.sendToClient(p.getPlayerUUID(), new WsMessage<>(PlayerPublicMapper.toPlayerPublicDto(p), WsMessageType.PLAYER_DATA));
                    break;
                }
//                p.resetBalanceDifference();
            }
        }

//        listener.broadcast(new MyPackage<>(TIME_FOR_DECISION, EMessageType.TIME_FOR_DECISION));
        tableModel.setDealerModel(new DealerModel());
        dealerModel = tableModel.getDealerModel();
        messageSenderImpl.broadcast(new WsMessage<>(/*gameSeats*/TableMapper.toDto(tableModel), WsMessageType.GAME_STARTED/*TABLE_STATUS*/));//mb send after resetGameResultStatus
        messageSenderImpl.broadcast(new WsMessage<>(DealerMapper.toDto(dealerModel), WsMessageType.DEALER));//TODO mb not to send the dealer (//mb send after resetGameResultStatus)
//        String nextGame;

        if (gameDeck == null) {
            gameDeck = new ArrayList<>(deckModelObject.getOneUsualDeck());//TODO mb change smth here
            Collections.shuffle(gameDeck);
        }

        for (SeatModel seatModel : gameSeatModels) { //TODO mb change PROGRESSING to null
            seatModel.resetGameResultStatus();
        }
        messageSenderImpl.broadcast(new WsMessage<>(RoundResult.PROGRESSING, WsMessageType.E_GAME_RESULT_STATUS));

//        dealer.resetGameResultStatus(); //TODO mb change PROGRESSING to null
//        listener.broadcast(new MyPackage<>(DealerMapper.toDto(dealer), EMessageType.DEALER));//TODO mb not to send the dealer

        System.out.println("Bets are closed, good luck!");

        changeGameStatusForInterface(GamePhaseUI.DEALING_CARDS);//TODO mb i dont need it

        System.out.println("\nInitial cards:");
        for (int i = 1; i <= COUNT_OF_INITIAL_CARDS; ++i) {
            for (SeatModel seatModel : gameSeatModels) {

                CardModel cardModel = gameDeck.removeLast();

                seatModel.calculateScore(cardModel);



                if (seatModel.getMainScore() == 21) {
                    System.out.println(cardModel.getInitial() + " of " + cardModel.getSuit() + " was " +
                            "dealt to player on seat #" + seatModel.getSeatNumber() + ", score - BLACKJACK (" + seatModel.getMainScore() + ")");
                    //TODO display it in the playersInGameSession' interface

                    seatModel.setRoundResult(RoundResult.BLACKJACK);
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

            CardModel cardModel = gameDeck.removeLast();


            if (i == 1) {
                System.out.println(cardModel.getInitial() + " of " + cardModel.getSuit() + " was " +
                        "dealt to '" + dealerModel.getNickName() + "', score = " + dealerModel.getScore());

                dealerModel.calculateScore(cardModel);
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

//        EDecision firstDecision;
        System.out.println("\nPlayer's decisions:");


        for (SeatModel seatModel : gameSeatModels) {

            while (seatModel.getMainScore() < 21) {

                GameDecision firstGameDecision = gettingDecision(seatModel);

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
                    seatModel.setLastGameDecision(firstGameDecision);
                    SeatDto seatDto = SeatMapper.toDto(seatModel);
                    messageSenderImpl.broadcast(new WsMessage<>(seatDto, WsMessageType.GAME_SEAT_UPDATED));
                    System.out.println(seatModel.getPlayerUUID() + " decided to " + firstGameDecision);

                    System.out.println(seatModel.getPlayerUUID() + " is standing on " + seatModel.getMainScore());

                    break;

                } else if (firstGameDecision.equals(GameDecision.HIT)) {
                    seatModel.setLastGameDecision(firstGameDecision);

                    System.out.println(seatModel.getPlayerUUID() + " decided to " + firstGameDecision);

                    GameDecision nextGameDecision;
                    boolean isStand = false;

                    do {
                        CardModel cardModel = gameDeck.removeLast();


                        seatModel.calculateScore(cardModel);


                        System.out.println(cardModel.getInitial() + " of " + cardModel.getSuit() + " was " +
                                "dealt to '" + seatModel.getPlayerUUID() + "'");

                        SeatDto seatDto = SeatMapper.toDto(seatModel);
                        messageSenderImpl.broadcast(new WsMessage<>(seatDto, WsMessageType.GAME_SEAT_UPDATED));

                        if (seatModel.getMainScore() < 21) {
                            do {
                                System.out.print(seatModel.getPlayerUUID() + " has " + seatModel.getMainScore() + ", what is your next decision? (hit, cash-out, stand) - ");//todo сделать, чтобы это предлагал дилер

                                nextGameDecision = gettingDecision(seatModel);
                                //TODO по идее нужна проверка на налл как и с первым решением
                                seatModel.setLastGameDecision(nextGameDecision);

                                if (nextGameDecision.equals(GameDecision.STAND)) {
                                    System.out.println(seatModel.getPlayerUUID() + " is standing on " + seatModel.getMainScore());
                                    isStand = true;

                                    break;
                                } else if (nextGameDecision.equals(GameDecision.CASH_OUT)) {
                                    System.out.println(seatModel.getPlayerUUID() + " CASHOUT");
                                    isStand = true;
                                    seatModel.setRoundResult(RoundResult.CASH_OUT);
                                    seatDto = SeatMapper.toDto(seatModel);
                                    messageSenderImpl.broadcast(new WsMessage<>(seatDto, WsMessageType.GAME_SEAT_UPDATED));

                                    break;
                                }
                            } while (!isValidNextDecision(nextGameDecision));
                        } else {
                            isStand = true;
                        }

                    } while (!isStand);

                    break;

                } else if (firstGameDecision.equals(GameDecision.DOUBLE_DOWN)) {

                    //money
                    PlayerModel curPlayer = null;
                    for (PlayerModel player : players) {
                        if (player.getPlayerUUID().equals(seatModel.getPlayerUUID())) {
                            curPlayer = player;
                        }
                    }

                    if (curPlayer == null) {
                        logger.error("curPlayer is null");
                        return null;
                    }

//                    curPlayer.changeBalance(-seat.getCurrentBet());//balance was changed
                    curPlayer.getUserModel().changeBalance(seatModel.getCurrentBet().negate());//balance was changed

                    SeatModel tmpSeatModel = null;
                    int tmpInd = -1;
                    for (SeatModel s : curPlayer.getSeatModels()) {
                        if (seatModel.getSeatNumber() == s.getSeatNumber()) {
                            tmpSeatModel = s;
                            tmpInd = curPlayer.getSeatModels().indexOf(s);
                        }
                    }

                    if (tmpSeatModel == null || tmpInd == -1) {
                        logger.error("tmpSeat or tmpInd is wrong");
                        return null;
                    }

                    curPlayer.getSeatModels().set(tmpInd, seatModel);

//                    seat.setCurrentBet(seat.getCurrentBet() * 2);
                    seatModel.setCurrentBet(seatModel.getCurrentBet().multiply(BigDecimal.valueOf(2)));
                    playersBroadcast();//TODO think here, coz in fact i dont need broadcast (i change only one Player)

                    seatModel.setLastGameDecision(firstGameDecision);
                    System.out.println(seatModel.getPlayerUUID() + " decided to " + firstGameDecision);

                    CardModel cardModel = gameDeck.removeLast();

                    seatModel.calculateScore(cardModel);

                    SeatDto seatDto = SeatMapper.toDto(seatModel);
                    messageSenderImpl.broadcast(new WsMessage<>(seatDto, WsMessageType.GAME_SEAT_UPDATED));

                    System.out.println(firstGameDecision + " for " + seatModel.getPlayerUUID());
                    System.out.println(cardModel.getInitial() + " of " + cardModel.getSuit() + " was " +
                            "dealt to '" + seatModel.getPlayerUUID() + "'");


                    if (seatModel.getMainScore() < 21) {
                        System.out.println(seatModel.getPlayerUUID() + " has " + seatModel.getMainScore());
                    }

                    break;

                }/* else if (firstGameDecision.equals(GameDecision.SPLIT)) {

                    //TODO finish this split option
                    //TODO finish this split option

                    seat.setLastGameDecision(firstGameDecision);
                    System.out.println(seat.getUserUUID() + " decided to " + firstGameDecision);
                    System.out.println(firstGameDecision + " for " + seat.getUserUUID());

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
                        if (p.getUserUUID().equals(seat.getUserUUID())) {
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
                    seatModel.setLastGameDecision(firstGameDecision);
                    System.out.println(seatModel.getPlayerUUID() + " cashed-out");
                    seatModel.setRoundResult(RoundResult.CASH_OUT);
                    SeatDto seatDto = SeatMapper.toDto(seatModel);
                    messageSenderImpl.broadcast(new WsMessage<>(seatDto, WsMessageType.GAME_SEAT_UPDATED));

                    break;
                }

            }
            if (seatModel.getMainScore() == 21 && seatModel.getMainHand().size() == 2) {
                System.out.println(seatModel.getPlayerUUID() + " has BLACKJACK (" + seatModel.getMainScore() + ") - amazing");

                seatModel.setRoundResult(RoundResult.BLACKJACK);//тк если у диллера тоже BJ, то у игрока PUSH
                SeatDto seatDto = SeatMapper.toDto(seatModel);
                messageSenderImpl.broadcast(new WsMessage<>(seatDto, WsMessageType.GAME_SEAT_UPDATED));

            }
            if (seatModel.getMainScore() == 21 && seatModel.getMainHand().size() > 2) {
                System.out.println(seatModel.getPlayerUUID() + " has " + seatModel.getMainScore() + " - good catch");
                SeatDto seatDto = SeatMapper.toDto(seatModel);
                messageSenderImpl.broadcast(new WsMessage<>(seatDto, WsMessageType.GAME_SEAT_UPDATED));

            }
            if (seatModel.getMainScore() > 21) {
                System.out.println(seatModel.getPlayerUUID() + " has TOO MANY (" + seatModel.getMainScore() + ") - sadly");

                seatModel.setRoundResult(RoundResult.LOSE);//как по мне - не особо правильно это тут распологать

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

        //check dealer's score and then appropriate actions (hit or stand)
        while (dealerModel.getScore() < 17) {

            System.out.println("hit for 'Dealer'");
            CardModel cardModel = gameDeck.removeLast();
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

        {//this block of code is for dealer's turn (after playersInGameSession)...
            System.out.println("Game results:");
            changeGameStatusForInterface(GamePhaseUI.RESULT_ANNOUNCEMENT);

            //check dealer's score - BJ
            if (dealerModel.getScore() == 21 && dealerModel.getHand().size() == 2) {
                System.out.println("Unfortunately, " + dealerModel.getNickName() + " has BLACKJACK (" + dealerModel.getScore() + ")");

                dealerModel.setRoundResult(RoundResult.BLACKJACK);
                messageSenderImpl.broadcast(new WsMessage<>(DealerMapper.toDto(dealerModel), WsMessageType.DEALER));


                gameSeatModels.stream()//TODO develop insurance if Dealer has ace
                        .filter(p -> p.getRoundResult() == RoundResult.BLACKJACK)
                        .forEach(p -> p.setRoundResult(RoundResult.PUSH));

                gameSeatModels.stream()
                        .filter(p -> p.getRoundResult() == RoundResult.PROGRESSING)
                        .forEach(p -> p.setRoundResult(RoundResult.LOSE));
            }

            //check dealer's score - less or equals 20
            if (dealerModel.getScore() <= 20) {
                System.out.println(dealerModel.getNickName() + " has " + dealerModel.getScore());

                gameSeatModels.stream()
                        .filter(p -> p.getMainScore() < dealerModel.getScore() &&
                                p.getRoundResult() == RoundResult.PROGRESSING)//отметка, что он к примеру не кєшанул
                        //или тп, а ещё в игре
                        .forEach(p -> p.setRoundResult(RoundResult.LOSE));

                gameSeatModels.stream()
                        .filter(p -> p.getMainScore() == dealerModel.getScore() &&
                                p.getRoundResult() == RoundResult.PROGRESSING)//отметка, что он к примеру не кєшанул
                        //или тп, а ещё в игре
                        .forEach(p -> p.setRoundResult(RoundResult.PUSH));

                gameSeatModels.stream()
                        .filter(p -> p.getMainScore() > dealerModel.getScore() &&
                                p.getRoundResult() == RoundResult.PROGRESSING)
                        .forEach(p -> p.setRoundResult(RoundResult.WIN));
            }

            //check dealer's score - quells 21 and not BJ
            if (dealerModel.getScore() == 21 && dealerModel.getHand().size() > 2) {
                System.out.println(dealerModel.getNickName() + " has " + dealerModel.getScore());

                gameSeatModels.stream()
                        .filter(p -> p.getMainScore() == 21 &&
                                p.getRoundResult() == RoundResult.PROGRESSING)
                        .forEach(p -> p.setRoundResult(RoundResult.PUSH));

                gameSeatModels.stream()
                        .filter(p -> p.getRoundResult() == RoundResult.PROGRESSING)
                        .forEach(p -> p.setRoundResult(RoundResult.LOSE));
            }

            //check dealer's score - more than 21 (too many)
            if (dealerModel.getScore() > 21) {
                System.out.println(dealerModel.getNickName() + " has TOO MANY (" + dealerModel.getScore() + ")");

                dealerModel.setRoundResult(RoundResult.BUST);
                messageSenderImpl.broadcast(new WsMessage<>(DealerMapper.toDto(dealerModel), WsMessageType.DEALER));


                gameSeatModels.stream()
                        .filter(p -> p.getMainScore() <= 21 &&
                                p.getRoundResult() == RoundResult.PROGRESSING)
                        .forEach(p -> p.setRoundResult(RoundResult.WIN));
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

        distributeMoney();
        //playersBroadcastCallback.playersBroadcast();//TODO as for me it's pointless coz i do it in distributeMoney() before this line


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

        List<PlayerSnapshot> playerSnapshots = players.stream()
                .map(player -> new PlayerSnapshot(
                        player.getUserModel(),
                        player.getSeatModels().stream()
                                .map(seat -> new SeatSnapshot(
                                        player.getUserModel().getUserUUID(),
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


        for (PlayerModel p : players) {
            for (SeatModel s : p.getSeatModels()) {
                s.fullSeatReset();
            }
            p.setWantsToStartGame(false);
//            p.resetBalanceDifference();
        }

        playersBroadcast();//need because of s.fullSeatReset() for every player

        gameSeatModels = null;

        //reset dealer's game data
        dealerModel.fullSeatReset();


        for (PlayerModel player : players) {
            if (player.getSeatModels().isEmpty()) {
                messageSenderImpl.sendToClient(player.getPlayerUUID(), new WsMessage<>(GamePhaseUI.EMPTY_TABLE, WsMessageType.E_GAME_STATUS_FOR_INTERFACE));
            } else {
                messageSenderImpl.sendToClient(player.getPlayerUUID(), new WsMessage<>(GamePhaseUI.PLACING_BETS, WsMessageType.E_GAME_STATUS_FOR_INTERFACE));
            }
        }

        for (PlayerModel p : players) {
            p.setInTheGame(false);
            messageSenderImpl.sendToClient(p.getPlayerUUID(), new WsMessage<>(PlayerPublicMapper.toPlayerPublicDto(p), WsMessageType.PLAYER_DATA));
        }
        tableModel.setDealerModel(null);//мб необязательно

        //TODO mb combine GAME_RESULT and GAME_FINISHED somehow...
        List<SeatDto> seatsDto = SeatMapper.toDtoList(tableModel.getSeatModels());
        messageSenderImpl.broadcast(new WsMessage<>(seatsDto, WsMessageType.GAME_FINISHED));//sending exactly busy seats at the table (not gameSeats)...
//        messageSenderImpl.broadcast(new WsMessage<>(table.getSeats(), WsMessageType.GAME_FINISHED));
        //TODO надо сделать так, чтобы играющий не мог занять ещё места пока не закончится игра

        //TODO (по поводу верхнего я хз как оно) пока что в этой реализации сделано так, что когда GAME _ FINISHED, то отправляется коллекция обычных мест
        //TODO чтобы играющие (и наблюдающие за игрой) получили актуальный список мест

        tableModel.setGame(false);
        isGameRunning = false;

        return gameResultSnapshot;
//        GameResult gameResult = new GameResult(playersInGameSession, dealerModel.getScore());
//        return playersInGameSession;
    }

    private boolean isValidNextDecision(GameDecision gameDecision) {
        return gameDecision.equals(GameDecision.HIT) ||
                gameDecision.equals(GameDecision.CASH_OUT) ||
                gameDecision.equals(GameDecision.STAND);
    }

    public void distributeMoney() {//TODO check if everything works properly
        if (players == null) {
            logger.error("Player collection is null");
            new Exception("Player collection is null").printStackTrace();
            return;
        }

        RoundResult result;
        for (SeatModel seatModel : gameSeatModels) {
            result = seatModel.getRoundResult();

            PlayerModel curPlayer = null;
            for (PlayerModel player : players) {
                if (player.getPlayerUUID().equals(seatModel.getPlayerUUID())) {
                    curPlayer = player;
                }
            }

            if (curPlayer == null) {
                logger.error("curPlayer is null");
                new Exception("curPlayer is null").printStackTrace();
                return;
            }

            if (result == RoundResult.CASH_OUT) {
//                curPlayer.changeBalance(seat.getCurrentBet() / 2);
                curPlayer.getUserModel().changeBalance(seatModel.getCurrentBet().divide(BigDecimal.valueOf(2)));
//                curPlayer.setBalanceDelta(curPlayer.getBalance());
            }

            if (result == RoundResult.LOSE) {
                //dealer.changeAmountOfMoney(seat.getCurrentBet()); //this is just for fun
            }

            if (result == RoundResult.LOSE) {
                //dealer.changeAmountOfMoney(seat.getCurrentBet()); //this is just for fun
            }

            if (result == RoundResult.WIN) {
//                curPlayer.changeBalance(seat.getCurrentBet() * 2);
                curPlayer.getUserModel().changeBalance(seatModel.getCurrentBet().multiply(BigDecimal.valueOf(2)));

            }

            if (result == RoundResult.BLACKJACK) {
//                curPlayer.changeBalance((int) (seat.getCurrentBet() * 2.5));//in general x1.5, but here is 2.5
                curPlayer.getUserModel().changeBalance(seatModel.getCurrentBet().multiply(BigDecimal.valueOf(2.5)));
            }

            if (result == RoundResult.PUSH) {
                curPlayer.getUserModel().changeBalance(seatModel.getCurrentBet());
            }
        }

/*        if (playersBroadcastCallback == null) {
            logger.error("playersBroadcastCallback is null");
            new Exception("playersBroadcastCallback is null").printStackTrace();
            return;
        }*/

        playersBroadcast();//if im not wrong - its for sending of results at the end of the game

        for (PlayerModel player : players) {
            System.err.println(player.getPlayerUUID() + " - balance - " + player.getUserModel().getBalance() + " delta - " + player.getUserModel().getBalanceDelta());
        }
    }


    public GameDecision gettingDecision(SeatModel seatModel) {
        //TODO display it in the playersInGameSession' interface
        messageSenderImpl.broadcast(new WsMessage<>(SeatMapper.toDto(seatModel), WsMessageType.CURRENT_SEAT));

        tableModel.setTurnOfPlayerId(seatModel.getPlayerUUID());
//        timerForDecision = new MyTimer();//TODO mb initialise not here...

/*        new Thread(() -> {
//            timer.startTimer(TIME_FOR_DECISION);
            timerService.start(TimerType.DECISION_TIME, 15, );

        }).start();*/
/*        timerService.start(TimerType.DECISION_TIME, TIME_FOR_DECISION, time -> {
            messageSender.broadcast(new MyPackage<>(time, EMessageType.TIMER));

            if (time == 0 && decisionField == null) {
                decisionField = basicDecision(seat);
            }
        });*/
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
//        System.out.println("AFTER empty while (waiting for a decision)");

        if (gameDecisionField != null /*&& timerForDecision.isRunning()*/) {
            System.out.println("Decision was made on time");
            return getGameDecisionField();
        }

        if (gameDecisionField == null /*&& !timerForDecision.isRunning()*/) {
            GameDecision gameDecision = basicDecision(seatModel);
            logger.info("decisionField == null and timer is over -> basicDecision - " + gameDecision);
            return gameDecision;
        }

        return null;
    }

    public GameDecision basicDecision(SeatModel seatModel) {
        if (seatModel.getMainScore() > 11) {
            return GameDecision.STAND;
//            seat.setCurrentDecision(EDecision.STAND);
        } else {
            return GameDecision.HIT;
//            seat.setCurrentDecision(EDecision.HIT);
        }
    }


    public boolean isAbleToSplit(SeatModel seatModel) {//idk
        return seatModel.getMainHand().getFirst().getCoefficient() ==
                seatModel.getMainHand().getLast().getCoefficient();
    }

    public boolean isFirstDecision(SeatModel seatModel) {//idk
        return seatModel.getLastGameDecision() == null;
    }

    //TODO playersBroadcast - bed violation!!!
    public void playersBroadcast() {//It's for sending to certain player his player data
        for (PlayerModel playerModel : players) {
            messageSenderImpl.sendToClient(playerModel.getPlayerUUID(), new WsMessage<>(PlayerPublicMapper.toPlayerPublicDto(playerModel), WsMessageType.PLAYER_DATA));
        }
    }
}
