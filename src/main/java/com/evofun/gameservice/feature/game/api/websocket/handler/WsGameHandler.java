package com.evofun.gameservice.feature.game.api.websocket.handler;

import com.evofun.gameservice.feature.game.api.dto.request.GameDecisionRequest;
import com.evofun.gameservice.feature.game.api.websocket.connection.WsClient;
import com.evofun.gameservice.feature.game.api.websocket.exception.GameValidationException;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessage;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessageSenderImpl;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessageType;
import com.evofun.gameservice.feature.game.api.websocket.service.ValidationService;
import com.evofun.gameservice.feature.game.application.service.GameService;
import com.evofun.gameservice.feature.game.application.service.TableService;
import com.evofun.gameservice.feature.game.domain.model.Seat;
import com.evofun.gameservice.feature.game.domain.model.enums.GameDecision;
import com.evofun.gameservice.feature.game.infrastructure.integration.MoneyServiceClient;
import com.evofun.gameservice.shared.error.ErrorCode;
import com.evofun.gameservice.shared.error.ErrorDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class WsGameHandler {
    private final ObjectMapper objectMapper;
    private final TableService tableService;
    private final GameService gameService;
    private final ValidationService validationService;
    private final MoneyServiceClient moneyServiceClient;
    private final WsMessageSenderImpl messageSenderImpl;

    public WsGameHandler(ObjectMapper objectMapper, TableService tableService, GameService gameService, ValidationService validationService, MoneyServiceClient moneyServiceClient, WsMessageSenderImpl messageSenderImpl) {
        this.objectMapper = objectMapper;
        this.tableService = tableService;
        this.gameService = gameService;
        this.validationService = validationService;
        this.moneyServiceClient = moneyServiceClient;
        this.messageSenderImpl = messageSenderImpl;
    }

    public void handleRequestToStartGame(WsMessage<?> wsMessage, WsClient wsClient) {
        if (gameService.isGameRunning()) {
            throw new GameValidationException("Game is already started.", "Player tried to start game, but it already started.");
        }

        if (!tableService.isTableReadyToStartGame()) {
            throw new GameValidationException("Table is not ready to start a game.", "Player tried to start game, but there's no ready seats to start a game.");
        }

        gameService.processRequestToStartGame(wsClient.getPlayerUUID());
    }

    public void handleGameDecision(WsMessage<?> wsMessage, WsClient wsClient) {
        if (!gameService.isGameRunning()) {
            throw new GameValidationException("Game was not started, so 'Game Decisions' are not available.", "'GAME_DECISION' was received from (" + wsClient.getPlayerUUID() + "), but game was not started.");
        }

        Seat turnOfSeat = tableService.getTurnOfSeat();

        if (!turnOfSeat.getPlayerId().equals(wsClient.getPlayerUUID())) {
            throw new GameValidationException("This is not your turn!", "Player tried to make decision, but it wasn't his turn.");
        }

        GameDecisionRequest dto = objectMapper.convertValue(wsMessage.getMessage(), GameDecisionRequest.class);

        validationService.validateRequest(dto);

        GameDecision gameDecision = dto.getGameDecision();

        if (gameDecision == GameDecision.DOUBLE_DOWN && turnOfSeat.getLastGameDecision() == null) {
            if (!moneyServiceClient.reserveMoneyForBet(wsClient.getPlayerUUID(), turnOfSeat.getCurrentBet())) {
                messageSenderImpl.sendToClient(turnOfSeat.getPlayerId(),
                        new WsMessage<>(
                                new ErrorDto(ErrorCode.GAME_RULE_VIOLATION, null, "You don't have enough money for DOUBLE_DOWN decision.", null),
                                WsMessageType.ERROR
                        ));

                return;
            }

            tableService.doubleDownBet();
        }

        gameService.setDecisionField(gameDecision);
    }
}