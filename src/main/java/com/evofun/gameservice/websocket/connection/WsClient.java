package com.evofun.gameservice.websocket.connection;

import com.evofun.gameservice.db.UserGameBalanceDto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;

/*@Getter
@Setter*/
public class WsClient {
    @Getter
    @Setter
    private WebSocketSession session = null;
    @Getter
    private WsConnectionStatus wsConnectionStatus = null;
    @Getter
    @Setter
    private UUID playerUUID = null;

    @Getter
    @Setter
    private boolean isAuthorized = false;

    @Getter
    @Setter
    private boolean isReadyToGetMessages = false;

    public WsClient(WebSocketSession session) {
        this.session = session;
        wsConnectionStatus = WsConnectionStatus.ALMOST_CONNECTED;
    }

    public void setConnectionStatusToConnect() {
        wsConnectionStatus = WsConnectionStatus.CONNECTED;
    }

    public void setConnectionStatusToDisconnect() {
        wsConnectionStatus = WsConnectionStatus.DISCONNECTED;
    }

    @Override
    public String toString() {
        return "WsClient{" +
                "session=" + session +
                ", wsConnectionStatus=" + wsConnectionStatus +
                ", userId=" + playerUUID +
                ", isAuthorized=" + isAuthorized +
                ", isReadyToGetMessages=" + isReadyToGetMessages +
                '}';
    }
}
