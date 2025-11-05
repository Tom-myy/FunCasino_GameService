package com.evofun.gameservice.feature.game.application.service;

import com.evofun.events.GameFinishedEvent;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessageSenderImpl;
import com.evofun.gameservice.feature.game.domain.core.GameEngine;
import com.evofun.gameservice.feature.game.domain.core.timer.BettingTimeObserver;
import com.evofun.gameservice.feature.game.domain.core.timer.DecisionTimeObserver;
import com.evofun.gameservice.feature.game.domain.core.timer.TimerService;
import com.evofun.gameservice.feature.game.domain.core.timer.TimerType;
import com.evofun.gameservice.feature.game.domain.model.Player;
import com.evofun.gameservice.feature.game.domain.model.Seat;
import com.evofun.gameservice.feature.game.domain.model.enums.GameDecision;
import com.evofun.gameservice.feature.game.infrastructure.kafka.KafkaProducer;
import com.evofun.gameservice.feature.game.utils.PlayerRegistry;
import com.evofun.gameservice.feature.result.application.GameResultUseCase;
import com.evofun.gameservice.feature.result.application.PlayerResultUseCase;
import com.evofun.gameservice.feature.result.application.model.GameResultSnapshot;
import com.evofun.gameservice.feature.result.application.model.GameResultStatus;
import com.evofun.gameservice.feature.result.application.model.PlayerSnapshot;
import com.evofun.gameservice.feature.result.application.model.SeatSnapshot;
import com.evofun.gameservice.feature.result.infrastructure.persistence.entity.GameResultEntity;
import com.evofun.gameservice.feature.result.infrastructure.persistence.entity.PlayerResultEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private final GameEngine gameEngine;
    private final GameResultUseCase gameResultUseCase;
    private final PlayerResultUseCase playerResultUseCase;
    private final KafkaProducer kafkaProducer;

    public GameService(BettingTimeObserver bettingTimeObserver, DecisionTimeObserver decisionTimeObserver, TableService tableService, TimerService timerService, PlayerRegistry playerRegistry, WsMessageSenderImpl messageSenderImpl, GameResultUseCase gameResultUseCase, PlayerResultUseCase playerResultUseCase, KafkaProducer kafkaProducer) {
        this.bettingTimeObserver = bettingTimeObserver;
        this.tableService = tableService;
        this.timerService = timerService;
        this.playerRegistry = playerRegistry;
        this.gameResultUseCase = gameResultUseCase;
        this.playerResultUseCase = playerResultUseCase;
        this.kafkaProducer = kafkaProducer;
        this.gameEngine = new GameEngine(tableService.getTable(), messageSenderImpl, timerService, playerRegistry, decisionTimeObserver);
    }

    public boolean isGameRunning() {
        return gameEngine.isGameRunning();
    }

    public void tryStartBettingTime() {
        if (!timerService.isRunning(TimerType.BETTING_TIME)) {
            if (tableService.isTableReadyToStartGame()) {
                bettingTimeObserver.setOnTimeout(() -> {
                    if (tableService.isTableReadyToStartGame() && !gameEngine.isGameRunning()) {
                        startGameAsync();
                    }
                });
                timerService.start(TimerType.BETTING_TIME,  bettingTimeObserver);
            }
        }
    }

    public void processRequestToStartGame(UUID clientUUID) {
        for (Player p : playerRegistry.getPlayers()) {
            if (clientUUID.equals(p.getUserId())) {
                p.setWantsToStartGame(true);
                //TODO think about when player wanted to start game (clicked button)
                // and then he left game - I need to uncheck his wish to start game
                break;
            }
        }

        List<Player> tmpPlayersWithBet = new ArrayList<>();

        for (Seat s : tableService.getCalculatedGameSeats()) {
            for (Player p : playerRegistry.getPlayers()) {
                if (s.getPlayerId().equals(p.getUserId())) {
                    tmpPlayersWithBet.add(p);
                    break;
                }
            }
        }

        boolean allPlayersWantsToStartGame = true;
        for (Player p : tmpPlayersWithBet) {
            if (!p.isWantsToStartGame()) {
                allPlayersWantsToStartGame = false;
                break;
            }
        }

        if (allPlayersWantsToStartGame) {
            startGameAsync();
        }
    }

    private void handleAfterGame(List<PlayerSnapshot> playerSnapshots) {
        if (playerSnapshots == null) {
            logger.error("Game result is null");
            return;
        }

        for (PlayerSnapshot playerSnapshot : playerSnapshots) {///if a player doesn't have profit (he lost) - do nothing
            if (!Objects.equals(playerSnapshot.getGameProfit(), BigDecimal.ZERO)) {
                GameFinishedEvent gameFinishedEvent = new GameFinishedEvent(
                        playerSnapshot.getUserId(),
                        playerSnapshot.getGameProfit()
                );

                kafkaProducer.sendGameFinishedEvent(gameFinishedEvent);
            }
        }
    }

    private void startGameAsync() {
        executor.submit(() -> {
            try {
                GameResultSnapshot gameResult = gameEngine.startGame();
                handleAfterGame(gameResult.getPlayersInGameSession());

                saveGameResultsIntoDb(gameResult);
            } catch (Exception e) {
                logger.error("Game failed", e);
            }
        });
    }

    private void saveGameResultsIntoDb(GameResultSnapshot gameResult) {
        UUID gameSessionId = UUID.randomUUID();
        GameResultEntity gameSession = new GameResultEntity(
                gameSessionId,
                gameResult.getDealerScore(),
                GameResultStatus.FINISHED);

        gameResultUseCase.saveGameResult(gameSession);

        List<PlayerResultEntity> playerResultEntityList = new ArrayList<>();
        for (PlayerSnapshot p : gameResult.getPlayersInGameSession()) {
            for (SeatSnapshot s : p.getSeats()) {
                playerResultEntityList.add(new PlayerResultEntity(
                        gameSessionId,
                        s.getSeatNumber(),
                        s.getPlayerUUID(),
                        s.getRoundResult(),
                        s.getCurrentBet()));
            }
        }

        playerResultUseCase.savePlayerResult(playerResultEntityList);
    }

    public void setDecisionField(GameDecision gameDecision) {
        gameEngine.setGameDecisionField(gameDecision);
    }
}