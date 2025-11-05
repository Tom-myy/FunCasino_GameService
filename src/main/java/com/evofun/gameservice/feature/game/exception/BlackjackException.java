package com.evofun.gameservice.feature.game.exception;

import com.evofun.gameservice.shared.AppException;

public class BlackjackException extends AppException {
    public BlackjackException(String developerMessage) {
        super(
                developerMessage,
                "Some exception interrupted the game, contact the support."
        );
    }
}