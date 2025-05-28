package com.evofun.gameservice.websocket.exception;

import com.evofun.gameservice.common.error.ExceptionUtils;
import lombok.Getter;
import lombok.ToString;

@ToString(callSuper = true)
public class GameValidationException extends RuntimeException {
    @Getter
    private final String code;
    private final String userMessage;
    private final String developerMessage;

    public GameValidationException(String userMessage, String developerMessage) {
        this(userMessage, developerMessage, ExceptionUtils.generateErrorId("VAL"));
    }

    private GameValidationException(String userMessage, String developerMessage, String code) {
        super(developerMessage);
        this.userMessage = userMessage;
        this.developerMessage = developerMessage;
        this.code = code;
    }

    @Override
    public String getMessage() {
        return developerMessage + " ERROR-CODE: " + code;
    }

    public String getUserMessage() {
        return userMessage + " ERROR-CODE: " + code;
    }
}