package com.evofun.gameservice.feature.game.api.websocket.message;

import com.evofun.gameservice.feature.game.api.websocket.connection.WsClient;
import java.util.UUID;

public interface WsMessageSender {
    void sendToClient(WsClient wsClient, WsMessage<?> message);
    void sendToClient(UUID playerUUID, WsMessage<?> message);
    void broadcast(WsMessage<?> message);
}
