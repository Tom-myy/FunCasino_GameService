package com.evofun.gameservice.websocket.handler;

import com.evofun.gameservice.common.error.ErrorCode;
import com.evofun.gameservice.common.error.ErrorPrefix;
import com.evofun.gameservice.common.error.ErrorDto;
import com.evofun.gameservice.common.error.ExceptionUtils;
import com.evofun.gameservice.dto.TableDto;
import com.evofun.gameservice.game.PlayerRegistry;
import com.evofun.gameservice.game.service.GameService;
import com.evofun.gameservice.game.service.TableService;
import com.evofun.gameservice.websocket.connection.WsClient;
import com.evofun.gameservice.websocket.connection.WsClientRegistry;
import com.evofun.gameservice.websocket.connection.WsConnectionStatus;
import com.evofun.gameservice.websocket.exception.ClientNotFoundException;
import com.evofun.gameservice.websocket.exception.handler.WsExceptionProcessor;
import com.evofun.gameservice.websocket.message.WsMessage;
import com.evofun.gameservice.websocket.message.WsMessageSender;
import com.evofun.gameservice.websocket.message.WsMessageType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.UUID;

@Component
public class MainWsHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(MainWsHandler.class);
    private final ObjectMapper objectMapper;
    private final WsClientRegistry clientRegistry;
    private final WsMessageDispatcher messageDispatcher;
    private final PlayerRegistry playerRegistry;
    private final WsMessageSender messageSender;
    private final GameService gameService;
    private final TableService tableService;
    private final WsExceptionProcessor exceptionProcessor;

    public MainWsHandler(WsClientRegistry clientRegistry, WsMessageDispatcher messageDispatcher, PlayerRegistry playerRegistry, WsMessageSender messageSender, GameService gameService, TableService tableService, ObjectMapper objectMapper, WsExceptionProcessor exceptionProcessor) {
        this.clientRegistry = clientRegistry;
        this.messageDispatcher = messageDispatcher;
        this.playerRegistry = playerRegistry;
        this.messageSender = messageSender;
        this.gameService = gameService;
        this.tableService = tableService;
        this.objectMapper = objectMapper;
        this.exceptionProcessor = exceptionProcessor;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        clientRegistry.addTemporaryClient(session.getId(), new WsClient(session));
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        WsMessage<?> wsMessage = parseMessage(session, message);
        if (wsMessage == null) return;

        WsClient wsClient;
        try {
            wsClient = clientRegistry.findClientBySessionOrThrow(session);
        } catch (ClientNotFoundException e) {
            logger.warn(e.getMessage());

            //TODO - send error to the publicc (here or by exceptionProcessor.process(...))

            //sending here...

            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (IOException closeEx) {
                logger.warn("Failed to close WS session after session invalidation", closeEx);
            }
            return;
        }

        logReceivedMessage(wsClient, session, wsMessage);

        if (!wsClient.isAuthorized() && WsMessageType.AUTHORIZATION != wsMessage.getWsMessageType()) {
            logger.warn("TEMP publicc tried to send unauthorized message: " + wsMessage.getWsMessageType());

            String code = ExceptionUtils.generateErrorId(ErrorPrefix.AUTH);
            ErrorDto errorDto = new ErrorDto(ErrorCode.FORBIDDEN, code, "Unauthorized", null);

            messageSender.sendToClient(wsClient, new WsMessage<>(
                    errorDto,
                    WsMessageType.ERROR
            ));
            return;
        }

        try {
            messageDispatcher.dispatch(wsMessage, wsClient);
        } catch (Exception e) {
            exceptionProcessor.process(e, wsClient);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        WsClient wsClient = clientRegistry.findClientBySessionOrThrow(session);
        if (wsClient.isAuthorized()) {
            new Thread(() -> {//TODO change to ExecutorService
                removeInactiveClient(session);
            }).start();
        }
    }

    private void removeInactiveClient(WebSocketSession session) {
        WsClient wsClient = clientRegistry.findClientBySessionOrThrow(session);
        wsClient.setReadyToGetMessages(false);

        wsClient.setConnectionStatusToDisconnect();

        while (gameService.isGameRunning()) {//TODO change to ExecutorService
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (wsClient.getWsConnectionStatus() == WsConnectionStatus.CONNECTED) {
            return;
        }

//        UUID clientUUID = clientRegistry.findAuthUUIDBySession(session);
        UUID clientUUID = wsClient.getPlayerUUID();

        tableService.removePlayerSeats(playerRegistry.findPlayerById(clientUUID));
        clientRegistry.removeAuthenticatedClient(clientUUID);
        playerRegistry.removePlayerByUUID(clientUUID);
        //сделать некий метод, который после отключения клиента будет проверять списки клиентов,
        //игроков и мест, и будет удалять отключившигося клиента оттуда

        logger.info("Client disconnected: {}", clientUUID);

//        messageSender.broadcast(new WsMessage<>(clientRegistry.getConnectedClientCount(), WsMessageType.CLIENT_COUNT));
//        messageSender.broadcast(new WsMessage<>(clientRegistry.getAuthenticatedClients().size(), WsMessageType.CLIENT_COUNT));
        tableService.setPlayerCount(clientRegistry.getAuthenticatedClients().size());

        TableDto dto = tableService.getTableDto();
        messageSender.broadcast(new WsMessage<>(dto, WsMessageType.TABLE_STATUS));
    }

    private WsMessage<?> parseMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        WsMessage<?> wsMessage;
        try {
            wsMessage = objectMapper.readValue(payload, new TypeReference<WsMessage<?>>() {});
        } catch (JsonProcessingException e) {
            WsClient wsClient = clientRegistry.findClientBySessionOrThrow(session);
            exceptionProcessor.process(e, wsClient);
            return null;
        }

        if(wsMessage.getMessage() == null) {
            String code = ExceptionUtils.generateErrorId(ErrorPrefix.JSON);

            ErrorDto errorDto = new ErrorDto(ErrorCode.INVALID_REQUEST_FORMAT, code, "Invalid JSON message format - inner message is null", null);

            WsClient wsClient = clientRegistry.findClientBySessionOrThrow(session);

            messageSender.sendToClient(wsClient, new WsMessage<>(errorDto, WsMessageType.ERROR));
            logger.warn("Invalid JSON message format - inner message is null from WS session (id: " + wsClient.getSession().getId() + ")");
            return null;
        }

        return wsMessage;
    }

    private void logReceivedMessage(WsClient wsClient, WebSocketSession session, WsMessage<?> wsMessage) {
        if (wsClient.getPlayerUUID() != null) {
            logger.info("Received ({}) from AUTH publicc UUID ({})", wsMessage.getWsMessageType(), wsClient.getPlayerUUID());
        } else {
            logger.info("Received ({}) from TEMP publicc session ID ({})", wsMessage.getWsMessageType(), session.getId());
        }
    }
}