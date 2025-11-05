package com.evofun.gameservice.feature.game.api.websocket.handler;

import com.evofun.gameservice.feature.game.api.dto.request.AuthRequest;
import com.evofun.gameservice.feature.game.api.dto.response.TableResponse;
import com.evofun.gameservice.feature.game.api.websocket.connection.WsClient;
import com.evofun.gameservice.feature.game.api.websocket.connection.WsClientRegistry;
import com.evofun.gameservice.feature.game.api.websocket.exception.InvalidTokenException;
import com.evofun.gameservice.feature.game.api.websocket.exception.TokenExpiredException;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessage;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessageSenderImpl;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessageType;
import com.evofun.gameservice.feature.game.api.websocket.service.ValidationService;
import com.evofun.gameservice.feature.game.api.websocket.service.WsPlayerConnectionService;
import com.evofun.gameservice.feature.game.application.service.TableService;
import com.evofun.gameservice.feature.game.domain.model.Player;
import com.evofun.gameservice.feature.game.infrastructure.mapper.PlayerMapper;
import com.evofun.gameservice.feature.game.utils.PlayerRegistry;
import com.evofun.gameservice.shared.security.jwt.JwtUser;
import com.evofun.gameservice.shared.security.jwt.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WsAuthHandler {
    private static final Logger logger = LoggerFactory.getLogger(WsAuthHandler.class);
    private final ObjectMapper objectMapper;
    private final WsMessageSenderImpl messageSenderImpl;
    private final TableService tableService;
    private final WsPlayerConnectionService authService;
    private final PlayerRegistry playerRegistry;
    private final WsClientRegistry clientRegistry;
    private final JwtUtil jwtUtil;
    private final ValidationService validationService;

    public WsAuthHandler(ObjectMapper objectMapper, WsMessageSenderImpl messageSenderImpl, TableService tableService, WsPlayerConnectionService wsPlayerConnectionService, PlayerRegistry playerRegistry, WsClientRegistry clientRegistry, JwtUtil jwtUtil, ValidationService validationService) {
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
        AuthRequest request = objectMapper.convertValue(wsMessage.getMessage(), AuthRequest.class);

        validationService.validateRequest(request);

        JwtUser jwtUser = validateToken(request.getAccessToken());


        Player player = authService.processLogin(wsClient, jwtUser);
        messageSenderImpl.sendToClient(wsClient, new WsMessage<>(PlayerMapper.toResponse(player), WsMessageType.AUTHORIZATION));

        wsClient.setReadyToGetMessages(true);

        tableService.addPlayerNickName(playerRegistry.findPlayerById(wsClient.getPlayerUUID()));

        tableService.setPlayerCount(clientRegistry.getAuthenticatedClients().size());

        TableResponse dto = tableService.getTableDto();
        messageSenderImpl.broadcast(new WsMessage<>(dto, WsMessageType.TABLE_STATUS));
    }

    private JwtUser validateToken(String token) {
        try {
            return jwtUtil.extractPayloadFromToken(token);
        } catch (ExpiredJwtException expired) {
            throw new TokenExpiredException("Expired JWT token");
        } catch (JwtException e) {
            throw new InvalidTokenException("JWT token invalid");
        }
    }
}