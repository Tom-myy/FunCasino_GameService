package com.evofun.gameservice.feature.game.api.websocket.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
