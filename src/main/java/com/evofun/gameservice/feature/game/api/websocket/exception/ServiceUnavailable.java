package com.evofun.gameservice.feature.game.api.websocket.exception;

import com.evofun.gameservice.shared.AppException;

public class ServiceUnavailable extends AppException {
    public ServiceUnavailable(String developerMessage, String userMessage) {
        super(developerMessage, userMessage);
    }
}