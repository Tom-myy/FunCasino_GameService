package com.evofun.gameservice.websocket.exception.handler;

import com.evofun.gameservice.common.error.ErrorCode;
import com.evofun.gameservice.common.error.ErrorDto;
import com.evofun.gameservice.common.error.ExceptionUtils;
import com.evofun.gameservice.common.error.FieldErrorDto;
import com.evofun.gameservice.exception.InvalidTokenException;
import com.evofun.gameservice.exception.TokenExpiredException;
import com.evofun.gameservice.exception.UserNotFoundException;
import com.evofun.gameservice.websocket.connection.WsClient;
import com.evofun.gameservice.websocket.exception.GameSystemException;
import com.evofun.gameservice.websocket.exception.GameValidationException;
import com.evofun.gameservice.websocket.exception.RemoteServiceException;
import com.evofun.gameservice.websocket.message.WsMessage;
import com.evofun.gameservice.websocket.message.WsMessageSenderImpl;
import com.evofun.gameservice.websocket.message.WsMessageType;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WsExceptionProcessor {
    private static final Logger logger = LoggerFactory.getLogger(WsExceptionProcessor.class);
    private final WsMessageSenderImpl sender;

    public WsExceptionProcessor(WsMessageSenderImpl sender) {
        this.sender = sender;
    }

    public void process(Exception e, /*UUID playerId*/WsClient wsClient) {//TODO publicc must handle it
        if (e instanceof GameValidationException gve) {//TODO mb delete (use ConstraintViolationException instead)
            ErrorDto errorDto = new ErrorDto(ErrorCode.GAME_RULE_VIOLATION, gve.getCode(), gve.getUserMessage(), null);

            sender.sendToClient(wsClient, new WsMessage<>(errorDto, WsMessageType.ERROR));
            logger.warn(gve.getMessage());

        } else if (e instanceof GameSystemException gse) {
            ErrorDto errorDto = new ErrorDto(ErrorCode.SYSTEM_ERROR, gse.getCode(), gse.getUserMessage(), null);

            sender.sendToClient(wsClient, new WsMessage<>(errorDto, WsMessageType.ERROR));
            logger.error(gse.getMessage(), gse);

        } else if (e instanceof TokenExpiredException tee) {

            String code = ExceptionUtils.generateErrorId("VAL");

            ErrorDto errorDto = new ErrorDto(ErrorCode.AUTHORIZATION, code, tee.getMessage(), null);

            sender.sendToClient(wsClient, new WsMessage<>(errorDto, WsMessageType.ERROR));
            logger.warn(tee.getMessage() + " for session (id: " + wsClient.getSession().getId() + ")");

        } else if (e instanceof InvalidTokenException ite) {

            String code = ExceptionUtils.generateErrorId("VAL");

            ErrorDto errorDto = new ErrorDto(ErrorCode.AUTHORIZATION, code, ite.getMessage(), null);

            sender.sendToClient(wsClient, new WsMessage<>(errorDto, WsMessageType.ERROR));
            logger.warn(ite.getMessage() + " for session (id: " + wsClient.getSession().getId() + ")");

        } else if (e instanceof UserNotFoundException unfe) {

            String code = ExceptionUtils.generateErrorId("SYS");

            ErrorDto errorDto = new ErrorDto(ErrorCode.SYSTEM_ERROR, code, "Some system error on the server. Try again later or contact support.", null);

            sender.sendToClient(wsClient, new WsMessage<>(errorDto, WsMessageType.ERROR));
            logger.error("ERROR-CODE: [{}] - {}", code, unfe.getMessage(), unfe);

        } else if (e instanceof RemoteServiceException rse) {

            String code = ExceptionUtils.generateErrorId("SYS");

            ErrorDto errorDto = new ErrorDto(ErrorCode.SYSTEM_ERROR, code, "Some system error on the server. Try again later or contact support.", null);

            sender.sendToClient(wsClient, new WsMessage<>(errorDto, WsMessageType.ERROR));
            logger.error("ERROR-CODE: [{}] - {}", code, rse.getMessage(), rse);

        } else if (e instanceof JsonProcessingException jpe) {

            String code = ExceptionUtils.generateErrorId("JSON");

            ErrorDto errorDto = new ErrorDto(ErrorCode.INVALID_REQUEST_FORMAT, code, "Invalid JSON message format", null);

            sender.sendToClient(wsClient, new WsMessage<>(errorDto, WsMessageType.ERROR));
            logger.warn("Failed to parse publicc message from WS session (id: " + wsClient.getSession().getId() + ")");

        } else if (e instanceof ConstraintViolationException cve) {
            List<FieldErrorDto> fieldErrors = ExceptionUtils.extractFieldErrors(cve);
            String code = ExceptionUtils.generateErrorId("VAL");

            ErrorDto errorDto = new ErrorDto(ErrorCode.VALIDATION_ERROR, code, "Validation failed", fieldErrors);

            sender.sendToClient(wsClient, new WsMessage<>(errorDto, WsMessageType.ERROR));
            logger.warn(errorDto.getErrors().toString());//mb to log reason in other way...

        } else {
            String unknownCode = ExceptionUtils.generateErrorId("UNKNOWN");
            String unknownUserMessage = "Some system error on the server. " +
                    "Try again later or contact support. ERROR-CODE: " + unknownCode;

            ErrorDto errorDto = new ErrorDto(ErrorCode.UNKNOWN_ERROR, unknownCode, unknownUserMessage, null);

            logger.error("Unknown error [{}]", unknownCode, e);
            sender.sendToClient(wsClient, new WsMessage<>(errorDto, WsMessageType.ERROR));
        }
    }


}