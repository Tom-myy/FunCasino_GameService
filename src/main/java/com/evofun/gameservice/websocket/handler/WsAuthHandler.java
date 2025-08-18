package com.evofun.gameservice.websocket.handler;

import com.evofun.gameservice.MoneyServiceClient;
import com.evofun.gameservice.db.UserGameBalanceDto;
import com.evofun.gameservice.dto.TableDto;
import com.evofun.gameservice.dto.request.AuthRequestDto;
import com.evofun.gameservice.exception.InvalidTokenException;
import com.evofun.gameservice.exception.TokenExpiredException;
import com.evofun.gameservice.game.PlayerModel;
import com.evofun.gameservice.game.PlayerRegistry;
import com.evofun.gameservice.game.service.TableService;
import com.evofun.gameservice.mapper.PlayerPublicMapper;
import com.evofun.gameservice.security.jwt.JwtUser;
import com.evofun.gameservice.security.jwt.JwtUtil;
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
import org.springframework.stereotype.Component;

@Component
public class WsAuthHandler {
    private static final Logger logger = LoggerFactory.getLogger(WsAuthHandler.class);
    private final MoneyServiceClient moneyServiceClient;
    private final ObjectMapper objectMapper;
    private final WsMessageSenderImpl messageSenderImpl;
    private final TableService tableService;
    private final WsPlayerConnectionService authService;
    private final PlayerRegistry playerRegistry;
    private final WsClientRegistry clientRegistry;
    private final JwtUtil jwtUtil;
    private final ValidationService validationService;

    public WsAuthHandler(MoneyServiceClient moneyServiceClient, ObjectMapper objectMapper, WsMessageSenderImpl messageSenderImpl, TableService tableService, WsPlayerConnectionService wsPlayerConnectionService, PlayerRegistry playerRegistry, WsClientRegistry clientRegistry, JwtUtil jwtUtil, ValidationService validationService) {
        this.moneyServiceClient = moneyServiceClient;
        this.objectMapper = objectMapper;
        this.messageSenderImpl = messageSenderImpl;
        this.tableService = tableService;
        this.authService = wsPlayerConnectionService;
        this.playerRegistry = playerRegistry;
        this.clientRegistry = clientRegistry;
        this.jwtUtil = jwtUtil;
        this.validationService = validationService;
    }

    public void handleAuthorization(WsMessage<?> wsMessage, WsClient wsClient) {
        AuthRequestDto request = objectMapper.convertValue(wsMessage.getMessage(), AuthRequestDto.class);

        validationService.validateRequestDto(request);

        JwtUser jwtUser = validateGameToken(request.getGameToken());

        UserGameBalanceDto userGameBalanceDto = moneyServiceClient.getGameBalanceByUserId(jwtUser.getUserId());//TODO

        PlayerModel playerModel = authService.processLogin(wsClient, jwtUser);
        playerModel.setBalance(userGameBalanceDto.getBalance());
        messageSenderImpl.sendToClient(wsClient, new WsMessage<>(PlayerPublicMapper.toPlayerPublicDto(playerModel), WsMessageType.AUTHORIZATION));

        wsClient.setReadyToGetMessages(true);

        tableService.addPlayerNickName(playerRegistry.findPlayerById(wsClient.getPlayerUUID()));

        tableService.setPlayerCount(clientRegistry.getAuthenticatedClients().size());
//        messageSenderImpl.broadcast(new WsMessage<>(clientRegistry.getAuthenticatedClients().size(), WsMessageType.CLIENT_COUNT));//TODO replace to Table (field)

        TableDto dto = tableService.getTableDto();
        messageSenderImpl.broadcast(new WsMessage<>(dto, WsMessageType.TABLE_STATUS));
    }

    private JwtUser validateGameToken(String gameToken) {
        try {
            return jwtUtil.extractPayloadFromGameToken(gameToken);
        } catch (ExpiredJwtException expired) {
            throw new TokenExpiredException("Expired JWT gameToken");
        } catch (JwtException e) {
            throw new InvalidTokenException("JWT gameToken invalid");
        }
    }
}