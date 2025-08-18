package com.evofun.gameservice.exception;

import lombok.Getter;

@Getter
public abstract class AppException extends RuntimeException {
    private final String developerMessage;
    private final String userMessage;

    public AppException(String developerMessage, String userMessage) {
        super(developerMessage);
        this.developerMessage = developerMessage;
        this.userMessage = userMessage;
    }
}