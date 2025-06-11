package com.evofun.gameservice.game.service;

import com.evofun.gameservice.db.*;
import com.evofun.gameservice.dto.UserPublicDto;
import com.evofun.gameservice.forGame.UserServiceRemote;
import com.evofun.gameservice.game.GameDecision;
//import com.evofun.gameservice.game.GameResult;
import com.evofun.gameservice.game.PlayerModel;
import com.evofun.gameservice.game.PlayerRegistry;
import com.evofun.gameservice.model.Game;
import com.evofun.gameservice.model.SeatModel;
import com.evofun.gameservice.game.timer.BettingTimeObserver;
import com.evofun.gameservice.game.timer.DecisionTimeObserver;
import com.evofun.gameservice.game.timer.TimerService;
import com.evofun.gameservice.game.timer.TimerType;
import com.evofun.gameservice.websocket.message.WsMessage;
import com.evofun.gameservice.websocket.message.WsMessageSenderImpl;
import com.evofun.gameservice.websocket.message.WsMessageType;
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
    private final UserServiceRemote userServiceRemote;
    private final GameSessionService gameSessionService;
    private final PlayerInGameSessionService playerInGameSessionService;

    public GameService(BettingTimeObserver bettingTimeObserver, DecisionTimeObserver decisionTimeObserver, TableService tableService, TimerService timerService, PlayerRegistry playerRegistry, WsMessageSenderImpl messageSenderImpl, UserServiceRemote userServiceRemote, GameSessionService gameSessionService, PlayerInGameSessionService playerInGameSessionService) {
        this.bettingTimeObserver = bettingTimeObserver;
        this.tableService = tableService;
        this.timerService = timerService;
        this.playerRegistry = playerRegistry;
        this.messageSenderImpl = messageSenderImpl;
        this.userServiceRemote = userServiceRemote;
        this.gameSessionService = gameSessionService;
        this.playerInGameSessionService = playerInGameSessionService;
        this.game = new Game(tableService.getTable(), messageSenderImpl, timerService, playerRegistry, decisionTimeObserver);
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
            if (clientUUID.equals(p.getPlayerUUID())) {
                p.setWantsToStartGame(true);//TODO think about when player wanted to start game (clicked button)
                // and then he left game - I need to uncheck his wish to start game
                break;
            }
        }

        List<PlayerModel> tmpPlayersWithBet = new ArrayList<>();

        for (SeatModel s : tableService.getCalculatedGameSeats()) {
            for (PlayerModel p : playerRegistry.getPlayerModels()) {
                if (s.getPlayerUUID().equals(p.getPlayerUUID())) {
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
            messageSenderImpl.sendToClient(newPlayerDto.getUserUUID(), new WsMessage<>(newPlayerDto, WsMessageType.USER_INFO_REFRESH));
        }*/
        List<UserPublicDto> newListUserPublicDto = userServiceRemote.updateUsersAfterGame(playerModels);
        if (newListUserPublicDto == null) {
            logger.error("DTO fetch failed");
            return;
        }

        for (UserPublicDto newUserPublicDto : newListUserPublicDto) {
            messageSenderImpl.sendToClient(newUserPublicDto.getPlayerUUID(), new WsMessage<>(newUserPublicDto, WsMessageType.USER_INFO_REFRESH));
        }
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

        List<SeatInGame> seatInGameList = new ArrayList<>();
        for (PlayerSnapshot p : gameResult.getPlayersInGameSession()) {
            for (SeatSnapshot s : p.getSeats() ) {
                seatInGameList.add(new SeatInGame(
                        gameSessionId,
                        s.getSeatNumber(),
                        s.getPlayerUUID(),
                        s.getRoundResult(),
                        s.getCurrentBet()));
            }
        }

        playerInGameSessionService.savePlayerInGameSession(seatInGameList);
    }

    public void setDecisionField(GameDecision gameDecision) {
        game.setGameDecisionField(gameDecision);
    }
}