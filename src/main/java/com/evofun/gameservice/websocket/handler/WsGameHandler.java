package com.evofun.gameservice.websocket.handler;


import com.evofun.gameservice.dto.request.GameDecisionRequestDto;
import com.evofun.gameservice.game.GameDecision;
import com.evofun.gameservice.game.service.GameService;
import com.evofun.gameservice.game.service.TableService;
import com.evofun.gameservice.websocket.connection.WsClient;
import com.evofun.gameservice.websocket.exception.GameValidationException;
import com.evofun.gameservice.websocket.message.WsMessage;
import com.evofun.gameservice.websocket.validation.ValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class WsGameHandler {
    private final ObjectMapper objectMapper;
    private final TableService tableService;
    private final GameService gameService;
    private final ValidationService validationService;

    public WsGameHandler(ObjectMapper objectMapper, TableService tableService, GameService gameService, ValidationService validationService) {
        this.objectMapper = objectMapper;
        this.tableService = tableService;
        this.gameService = gameService;
        this.validationService = validationService;
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

        if(!tableService.getTurnOfPlayerId().equals(wsClient.getPlayerUUID())) {
            throw new GameValidationException("This is not your turn!", "Player tried to make decision, but it wasn't his turn.");
        }

        GameDecisionRequestDto dto = objectMapper.convertValue(wsMessage.getMessage(), GameDecisionRequestDto.class);

        validationService.validate(dto);

        GameDecision gameDecision = dto.getGameDecision();
        gameService.setDecisionField(gameDecision);
    }
}