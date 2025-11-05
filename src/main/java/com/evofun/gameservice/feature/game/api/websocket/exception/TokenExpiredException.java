package com.evofun.gameservice.feature.game.api.websocket.exception;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}
