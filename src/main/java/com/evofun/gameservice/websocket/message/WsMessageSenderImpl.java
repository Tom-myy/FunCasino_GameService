package com.evofun.gameservice.websocket.message;

import com.evofun.gameservice.websocket.connection.WsClient;
import com.evofun.gameservice.websocket.connection.WsClientRegistry;
import com.evofun.gameservice.websocket.connection.WsConnectionStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import java.util.UUID;

@Component
public class WsMessageSenderImpl implements WsMessageSender {//TODO it can throw exceptions
    private static final Logger logger = LoggerFactory.getLogger(WsMessageSenderImpl.class);
    private final ObjectMapper objectMapper;
    private final WsClientRegistry wsClientRegistry;

    public WsMessageSenderImpl(WsClientRegistry wsClientRegistry, ObjectMapper objectMapper) {
        this.wsClientRegistry = wsClientRegistry;
        this.objectMapper = objectMapper;
    }


    @Override
    public void sendToClient(WsClient wsClient, WsMessage<?> message) {
        String responseJson;
        try {
            responseJson = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (message.getWsMessageType() == WsMessageType.GAME_SEAT_UPDATED && !wsClient.isReadyToGetMessages()) return;

        try {
            synchronized (wsClient.getSession()) {
                wsClient.getSession().sendMessage(new TextMessage(responseJson));
            }
            logger.info("SendToClient (" + wsClient.getSession().getId() + " - session ID (not UUID)): msg_json - " + responseJson);
        } catch (Exception e) {
            if (wsClient.getWsConnectionStatus() == WsConnectionStatus.DISCONNECTED) {

            } else
                e.printStackTrace();
        }
    }

    @Override
    public void sendToClient(UUID playerUUID, WsMessage<?> message) {//TODO delete - use WsClient (clean Game and etc. from it...)
        String responseJson;
        try {
            responseJson = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        WsClient wsClient = wsClientRegistry.findAuthClientByUUID(playerUUID);

        if (wsClient == null) {
            logger.error("Client not found for UUID: " + playerUUID);
            return;
        }

        if (message.getWsMessageType() == WsMessageType.GAME_SEAT_UPDATED && !wsClient.isReadyToGetMessages()) return;

        try {
            synchronized (wsClient.getSession()) {
                wsClient.getSession().sendMessage(new TextMessage(responseJson));
            }
            logger.info("SendToClient (" + playerUUID + "): msg_json - " + responseJson);
        } catch (Exception e) {
            if (wsClient.getWsConnectionStatus() == WsConnectionStatus.DISCONNECTED) {
                //mb todo smth
            } else
                e.printStackTrace();
        }
    }

    @Override
    public void broadcast(WsMessage<?> message) {
        String responseJson;
        try {
            responseJson = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            logger.error("Error during up-casting for broadcasting", e);
            return;
        }

        synchronized (wsClientRegistry.getAuthenticatedClients()) {
            for (WsClient wsClient : wsClientRegistry.getAuthenticatedClients().values()) {
                if (message.getWsMessageType() == WsMessageType.GAME_SEAT_UPDATED && !wsClient.isReadyToGetMessages())
                    continue;

                try {
                    synchronized (wsClient.getSession()) {
                        wsClient.getSession().sendMessage(new TextMessage(responseJson));
                    }
                    logger.info("Broadcast to (" + wsClient.getPlayerUUID() + "): msg_json - " + responseJson);
                } catch (Exception e) {
                    if (wsClient.getWsConnectionStatus() == WsConnectionStatus.DISCONNECTED) {
                        //mb todo smth
                    } else {
                        e.printStackTrace();
                        logger.error("Error while broadcasting: " + responseJson, e);
                    }
                }
            }
        }
    }
}
