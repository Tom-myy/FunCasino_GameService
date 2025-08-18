package com.evofun.gameservice.websocket.exception;

import com.evofun.gameservice.common.error.ErrorPrefix;
import com.evofun.gameservice.common.error.ExceptionUtils;
import lombok.Getter;
import lombok.ToString;

@ToString(callSuper = true)
public class GameSystemException extends RuntimeException {
    private static final String USER_DEFAULT_DESCRIPTION = "Some system error on the server. Try again later or contact support.";
    @Getter
    private final String code;
    private final String developerMessage;

    public GameSystemException(String developerMessage) {
        super(developerMessage);
        this.code = ExceptionUtils.generateErrorId(ErrorPrefix.SYS);
        this.developerMessage = developerMessage;
    }

    @Override
    public String getMessage() {
        return developerMessage + " ERROR-CODE: " + code;
    }

    public String getUserMessage() {
        return USER_DEFAULT_DESCRIPTION + " ERROR-CODE: " + code;
    }
}