package com.evofun.gameservice.websocket.handler;

import com.evofun.gameservice.websocket.connection.WsClient;
import com.evofun.gameservice.websocket.message.WsMessage;
import com.evofun.gameservice.websocket.message.WsMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WsMessageDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(WsMessageDispatcher.class);
    private final WsAuthHandler wsAuthHandler;
    private final WsSeatHandler wsSeatHandler;
    private final WsGameHandler wsGameHandler;

    public WsMessageDispatcher(WsAuthHandler wsAuthHandler, WsSeatHandler wsSeatHandler, WsGameHandler wsGameHandler) {
        this.wsAuthHandler = wsAuthHandler;
        this.wsSeatHandler = wsSeatHandler;
        this.wsGameHandler = wsGameHandler;
    }

    public void dispatch(WsMessage<?> wsMessage, WsClient wsClient) {
        WsMessageType wsMessageType = wsMessage.getWsMessageType();

        switch (wsMessageType) {
            case AUTHORIZATION -> wsAuthHandler.handleAuthorization(wsMessage, wsClient);//TODO code refactoring

            case TAKE_SEAT -> wsSeatHandler.handleTakeSeat(wsMessage, wsClient);
            case LEAVE_SEAT -> wsSeatHandler.handleLeaveSeat(wsMessage, wsClient);
            case UPDATE_SEAT_BET -> wsSeatHandler.handleUpdateSeatBet(wsMessage, wsClient);

            case REQUEST_TO_START_GAME -> wsGameHandler.handleRequestToStartGame(wsMessage, wsClient);
            case GAME_DECISION -> wsGameHandler.handleGameDecision(wsMessage, wsClient);

            default -> logger.warn("Server got a unknown MESSAGE type!");
        }
    }
}
