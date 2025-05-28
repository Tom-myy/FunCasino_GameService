package com.evofun.gameservice.websocket.connection;

import com.evofun.gameservice.websocket.exception.ClientNotFoundException;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Component
public class WsClientRegistry {
    private final Map<String, WsClient> temporaryClients = new ConcurrentHashMap<>();
    private final Map<UUID, WsClient> authenticatedClients = new ConcurrentHashMap<>();

    public void addTemporaryClient(String sessionId, WsClient wsClient) {
        temporaryClients.put(sessionId, wsClient);
    }

    private WsClient findTempClientBySession(WebSocketSession session) {
        for (Map.Entry<String, WsClient> entry: temporaryClients.entrySet()){
            if (entry.getValue().getSession().equals(session)){
                return entry.getValue();
            }
        }
        return null;
    }

    public void promoteToAuthenticated(WsClient wsClient, UUID uuid) {
        temporaryClients.remove(wsClient.getSession().getId());
        wsClient.setPlayerUUID(uuid);
        wsClient.setAuthorized(true);
        authenticatedClients.put(uuid, wsClient);
    }

    public WsClient findAuthClientByUUID(UUID clientUUID) {
        return authenticatedClients.get(clientUUID);
    }

    private WsClient findAuthClientBySession(WebSocketSession session) {
        for (Map.Entry<UUID, WsClient> entry: authenticatedClients.entrySet()){
            if (entry.getValue().getSession().equals(session)){
                return entry.getValue();
            }
        }
        return null;
    }

    public void reconnectClient(WsClient wsClient, WebSocketSession session, UUID playerUUID) {
        wsClient.setPlayerUUID(playerUUID);
        wsClient.setConnectionStatusToConnect();
        wsClient.setSession(session);

        temporaryClients.remove(wsClient.getSession().getId());

        authenticatedClients.put(playerUUID, wsClient);
//        wsClient.setReadyToGetMessages(true);
    }

    public void removeAuthenticatedClient(UUID uuid) {
        authenticatedClients.remove(uuid);
    }

    public int getConnectedClientCount() {
        int count = 0;
        for (WsClient c :authenticatedClients.values()) {
            if (c.getWsConnectionStatus() == WsConnectionStatus.CONNECTED) ++count;
        }
        return count;
    }

    public WsClient findClientBySessionOrThrow(WebSocketSession session) {
        WsClient client = findAuthClientBySession(session);
        if (client != null) return client;

        client = findTempClientBySession(session);
        if (client != null) return client;

        /// (if my reconnect doesn't work properly) it's
        /// possible -> the publicc must completely reconnect to the WS
        throw new ClientNotFoundException(session.getId());
    }



}
