package com.evofun.gameservice.exception;

public class ServiceUnavailable extends AppException {
    public ServiceUnavailable(String developerMessage, String userMessage) {
        super(developerMessage, userMessage);
    }
}