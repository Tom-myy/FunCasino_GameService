package com.evofun.gameservice.feature.game.api.websocket.exception;

import com.evofun.gameservice.shared.AppException;

public class NotEnoughBalanceException extends AppException {
  public NotEnoughBalanceException(String developerMessage, String userMessage) {
    super(developerMessage, userMessage);
  }
}