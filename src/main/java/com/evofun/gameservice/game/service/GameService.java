package com.evofun.gameservice.game.service;

import com.evofun.events.GameFinishedEvent;
import com.evofun.gameservice.MoneyServiceClient;
import com.evofun.gameservice.db.*;
import com.evofun.gameservice.game.GameDecision;
//import com.evofun.gameservice.game.GameResult;
import com.evofun.gameservice.game.PlayerModel;
import com.evofun.gameservice.game.PlayerRegistry;
import com.evofun.gameservice.kafka.KafkaProducer;
import com.evofun.gameservice.model.Game;
import com.evofun.gameservice.model.SeatModel;
import com.evofun.gameservice.game.timer.BettingTimeObserver;
import com.evofun.gameservice.game.timer.DecisionTimeObserver;
import com.evofun.gameservice.game.timer.TimerService;
import com.evofun.gameservice.game.timer.TimerType;
import com.evofun.gameservice.websocket.message.WsMessageSenderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class GameService {
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final BettingTimeObserver bettingTimeObserver;
    private final TableService tableService;
    private final TimerService timerService;
    private final PlayerRegistry playerRegistry;
    private final WsMessageSenderImpl messageSenderImpl;
    private final Game game;
    private final GameSessionService gameSessionService;
    private final PlayerInGameSessionService playerInGameSessionService;
    private final KafkaProducer kafkaProducer;
    private final MoneyServiceClient moneyServiceClient;


    public GameService(BettingTimeObserver bettingTimeObserver, DecisionTimeObserver decisionTimeObserver, TableService tableService, TimerService timerService, PlayerRegistry playerRegistry, WsMessageSenderImpl messageSenderImpl, GameSessionService gameSessionService, PlayerInGameSessionService playerInGameSessionService, KafkaProducer kafkaProducer, MoneyServiceClient moneyServiceClient) {
        this.bettingTimeObserver = bettingTimeObserver;
        this.tableService = tableService;
        this.timerService = timerService;
        this.playerRegistry = playerRegistry;
        this.messageSenderImpl = messageSenderImpl;
        this.gameSessionService = gameSessionService;
        this.playerInGameSessionService = playerInGameSessionService;
        this.kafkaProducer = kafkaProducer;
        this.moneyServiceClient = moneyServiceClient;
        this.game = new Game(tableService.getTable(), messageSenderImpl, timerService, playerRegistry, decisionTimeObserver, moneyServiceClient);
    }

    public boolean isGameRunning() {
        return game.isGameRunning();
    }

    /*    public void tryStartBettingTime() {
            if (!timerService.isRunning(TimerType.BETTING_TIME)) {
                if (tableService.isTableReadyToStartGame()) {
                    timerService.start(TimerType.BETTING_TIME, 10, time -> {
                        if (time == 0 && tableService.isTableReadyToStartGame()) {
                            startGame();
                        } else {
                            messageSender.broadcast(new MyPackage<>(time, EMessageType.TIMER));
                        }
                    });
                }
            }
        }*/
    public void tryStartBettingTime() {
        if (!timerService.isRunning(TimerType.BETTING_TIME)) {
            if (tableService.isTableReadyToStartGame()) {
                bettingTimeObserver.setOnTimeout(() -> {
                    if (tableService.isTableReadyToStartGame() && !game.isGameRunning()) {
//                        startGame();
                        startGameAsync();
                    }
                });
                timerService.start(TimerType.BETTING_TIME, /*10,*/ bettingTimeObserver);
            }
        }
    }

    public void processRequestToStartGame(UUID clientUUID) {
        for (PlayerModel p : playerRegistry.getPlayerModels()) {
            if (clientUUID.equals(p.getUserId())) {
                p.setWantsToStartGame(true);//TODO think about when player wanted to start game (clicked button)
                // and then he left game - I need to uncheck his wish to start game
                break;
            }
        }

        List<PlayerModel> tmpPlayersWithBet = new ArrayList<>();

        for (SeatModel s : tableService.getCalculatedGameSeats()) {
            for (PlayerModel p : playerRegistry.getPlayerModels()) {
                if (s.getPlayerId().equals(p.getUserId())) {
                    tmpPlayersWithBet.add(p);
                    break;
                }
            }
        }

        boolean allPlayersWantsToStartGame = true;
        for (PlayerModel p : tmpPlayersWithBet) {
            if (!p.isWantsToStartGame()) {
                allPlayersWantsToStartGame = false;
                break;
            }
        }

        if (allPlayersWantsToStartGame) {
            startGameAsync();
        }
    }

    private void handleAfterGame(List<PlayerSnapshot> playerModels) {
        if (playerModels == null) {
            logger.error("Game result is null");
            return;
        }

        for (PlayerSnapshot playerSnapshot : playerModels) {
            GameFinishedEvent gameFinishedEvent = new GameFinishedEvent(
                    playerSnapshot.getUserId(),
                    playerSnapshot.getBalanceDelta(),
                    "GAME",//TODO enum
                    null);

            kafkaProducer.sendGameFinishedEvent(gameFinishedEvent);
        }

/*        List<PlayerModel> updated = userService.updateUsersAfterGame(playerModels);
        if (updated == null) {
            logger.error("Update failed");
            return;
        }

        List<UserDto> dtos = userService.getUpdatedUsers(updated);
        if (dtos == null) {
            logger.error("DTO fetch failed");
            return;
        }*/




/*        List<PlayerDto> newListPlayerDto = userServiceRemote.updateUsersAfterGame(playerModels);
        if (newListPlayerDto == null) {
            logger.error("DTO fetch failed");
            return;
        }

        for (PlayerDto newPlayerDto : newListPlayerDto) {
            messageSenderImpl.sendToClient(newPlayerDto.getUserId(), new WsMessage<>(newPlayerDto, WsMessageType.USER_INFO_REFRESH));
        }*/
/*        List<UserPublicDto> newListUserPublicDto = userBalanceRemote.updateUsersAfterGame(playerModels);
        if (newListUserPublicDto == null) {
            logger.error("DTO fetch failed");
            return;
        }

        for (UserPublicDto newUserPublicDto : newListUserPublicDto) {
            messageSenderImpl.sendToClient(newUserPublicDto.getUserId(), new WsMessage<>(newUserPublicDto, WsMessageType.USER_INFO_REFRESH));
        }*/
    }

    private void startGameAsync() {
        executor.submit(() -> {
            try {
//                List<PlayerModel> result = game.startGame();
                GameResultSnapshot gameResult = game.startGame();
                handleAfterGame(gameResult.getPlayersInGameSession());

                //save game result into DB
                saveGameResultsIntoDb(gameResult);
            } catch (Exception e) {
                logger.error("Game failed", e);
            }
        });
    }

    private void saveGameResultsIntoDb(GameResultSnapshot gameResult) {
        UUID gameSessionId = UUID.randomUUID();
        GameSession gameSession = new GameSession(
                gameSessionId,
                gameResult.getDealerScore(),
                GameSessionStatus.FINISHED);

        gameSessionService.saveGame(gameSession);

        List<GameSessionSeat> gameSessionSeatList = new ArrayList<>();
        for (PlayerSnapshot p : gameResult.getPlayersInGameSession()) {
            for (SeatSnapshot s : p.getSeats() ) {
                gameSessionSeatList.add(new GameSessionSeat(
                        gameSessionId,
                        s.getSeatNumber(),
                        s.getPlayerUUID(),
                        s.getRoundResult(),
                        s.getCurrentBet()));
            }
        }

        playerInGameSessionService.savePlayerInGameSession(gameSessionSeatList);
    }

    public void setDecisionField(GameDecision gameDecision) {
        game.setGameDecisionField(gameDecision);
    }
}