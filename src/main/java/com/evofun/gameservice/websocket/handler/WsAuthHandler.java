package com.evofun.gameservice.websocket.handler;

import com.evofun.gameservice.dto.TableDto;
import com.evofun.gameservice.dto.UserInternalDto;
import com.evofun.gameservice.dto.request.AuthRequestDto;
import com.evofun.gameservice.exception.InvalidTokenException;
import com.evofun.gameservice.exception.TokenExpiredException;
import com.evofun.gameservice.forGame.UserServiceRemote;
import com.evofun.gameservice.game.PlayerModel;
import com.evofun.gameservice.game.PlayerRegistry;
import com.evofun.gameservice.game.service.TableService;
import com.evofun.gameservice.security.jwt.JwtPayload;
import com.evofun.gameservice.security.jwt.JwtUtil;
import com.evofun.gameservice.mapper.PlayerInternalMapper;
import com.evofun.gameservice.mapper.UserInternalMapper;
import com.evofun.gameservice.model.UserModel;
import com.evofun.gameservice.websocket.connection.WsClient;
import com.evofun.gameservice.websocket.connection.WsClientRegistry;
import com.evofun.gameservice.websocket.handler.service.WsPlayerConnectionService;
import com.evofun.gameservice.websocket.message.WsMessage;
import com.evofun.gameservice.websocket.message.WsMessageSenderImpl;
import com.evofun.gameservice.websocket.message.WsMessageType;
import com.evofun.gameservice.websocket.validation.ValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class WsAuthHandler {
    private static final Logger logger = LoggerFactory.getLogger(WsAuthHandler.class);
    private final UserServiceRemote userServiceRemote;
    private final ObjectMapper objectMapper;
    private final WsMessageSenderImpl messageSenderImpl;
    private final TableService tableService;
    private final WsPlayerConnectionService authService;
    private final PlayerRegistry playerRegistry;
    private final WsClientRegistry clientRegistry;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ValidationService validationService;


    public WsAuthHandler(UserServiceRemote userServiceRemote, ObjectMapper objectMapper, WsMessageSenderImpl messageSenderImpl, TableService tableService, WsPlayerConnectionService wsPlayerConnectionService, PlayerRegistry playerRegistry, WsClientRegistry clientRegistry, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, ValidationService validationService) {
        this.userServiceRemote = userServiceRemote;
        this.objectMapper = objectMapper;
        this.messageSenderImpl = messageSenderImpl;
        this.tableService = tableService;
        this.authService = wsPlayerConnectionService;
        this.playerRegistry = playerRegistry;
        this.clientRegistry = clientRegistry;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.validationService = validationService;
    }

    public void handleAuthorization(WsMessage<?> wsMessage, WsClient wsClient) {
        AuthRequestDto request = objectMapper.convertValue(wsMessage.getMessage(), AuthRequestDto.class);

        validationService.validate(request);

        JwtPayload payload;
        try {
            payload = jwtUtil.extractPayload(request.getToken());
        } catch (ExpiredJwtException expired) {
            throw new TokenExpiredException("Expired JWT token");
        } catch (JwtException e) {
            throw new InvalidTokenException("JWT token invalid");
        }

        UserInternalDto userInternalDto = userServiceRemote.findUserById(payload.userId());//TODO

        UserModel userModel = UserInternalMapper.toModel(userInternalDto);

        PlayerModel playerModel = authService.processLogin(wsClient, userModel);
        messageSenderImpl.sendToClient(wsClient, new WsMessage<>(PlayerInternalMapper.toPlayerInternalDto(playerModel), WsMessageType.AUTHORIZATION));

        wsClient.setReadyToGetMessages(true);

        tableService.addPlayerNickName(playerRegistry.findPlayerByUUID(wsClient.getPlayerUUID()));

        tableService.setPlayerCount(clientRegistry.getAuthenticatedClients().size());
//        messageSenderImpl.broadcast(new WsMessage<>(clientRegistry.getAuthenticatedClients().size(), WsMessageType.CLIENT_COUNT));//TODO replace to Table (field)

        TableDto dto = tableService.getTableDto();
        messageSenderImpl.broadcast(new WsMessage<>(dto, WsMessageType.TABLE_STATUS));
    }
}