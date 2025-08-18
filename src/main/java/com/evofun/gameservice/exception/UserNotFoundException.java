package com.evofun.gameservice.exception;

public class UserNotFoundException extends AppException {
    public UserNotFoundException(String developerMessage, String userMessage) {
        super(developerMessage, userMessage);
    }
}