package com.evofun.gameservice.game;

import com.evofun.gameservice.exception.AppException;

public class BlackjackException extends AppException {
    public BlackjackException(String developerMessage) {
        super(
                developerMessage,
                "Some exception interrupted the game, contact the support."
        );
    }
}