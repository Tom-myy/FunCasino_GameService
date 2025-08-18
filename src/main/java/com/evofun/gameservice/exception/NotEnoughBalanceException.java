package com.evofun.gameservice.exception;

public class NotEnoughBalanceException extends AppException {
  public NotEnoughBalanceException(String developerMessage, String userMessage) {
    super(developerMessage, userMessage);
  }
}