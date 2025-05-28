package com.evofun.gameservice.websocket.exception;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(String sessionId) {
        super("WsClient not found for sessionId: " + sessionId);
    }
}
