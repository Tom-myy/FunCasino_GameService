package com.evofun.gameservice.websocket.handler;

import com.evofun.gameservice.MoneyServiceClient;
import com.evofun.gameservice.dto.request.GameDecisionRequestDto;
import com.evofun.gameservice.exception.NotEnoughBalanceException;
import com.evofun.gameservice.game.GameDecision;
import com.evofun.gameservice.game.service.GameService;
import com.evofun.gameservice.game.service.TableService;
import com.evofun.gameservice.model.SeatModel;
import com.evofun.gameservice.websocket.connection.WsClient;
import com.evofun.gameservice.websocket.exception.GameValidationException;
import com.evofun.gameservice.websocket.message.WsMessage;
import com.evofun.gameservice.websocket.message.WsMessageSenderImpl;
import com.evofun.gameservice.websocket.message.WsMessageType;
import com.evofun.gameservice.websocket.validation.ValidationService;
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
        /// mb check smth else here...
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
            throw new GameValidationException("Game was not started, so 'Game Decisions' are not available.", "'GAME_DECISION' was received from ("+ wsClient.getPlayerUUID()+"), but game was not started.");
        }

        SeatModel seat = tableService.getTurnOfSeat();

        if(!seat.getPlayerId().equals(wsClient.getPlayerUUID())) {
            throw new GameValidationException("This is not your turn!", "Player tried to make decision, but it wasn't his turn.");
        }

        GameDecisionRequestDto dto = objectMapper.convertValue(wsMessage.getMessage(), GameDecisionRequestDto.class);

        validationService.validateRequestDto(dto);

        GameDecision gameDecision = dto.getGameDecision();

        if(gameDecision == GameDecision.DOUBLE_DOWN) {
            if(!moneyServiceClient.reserveMoneyForBet(wsClient.getPlayerUUID(),  seat.getCurrentBet())) {
                messageSenderImpl.sendToClient(seat.getPlayerId(),
                        new WsMessage<>(
                                "You don't have enough money for DOUBLE_DOWN decision.",
                                WsMessageType.ERROR
                        ));

                return;
            }

            tableService.doubleDownBet();//TODO mb to do smth here
        }
        
        gameService.setDecisionField(gameDecision);
    }
}