package com.evofun.gameservice.feature.game.api.websocket.service;

import com.evofun.gameservice.feature.game.domain.model.Player;
import com.evofun.gameservice.feature.game.utils.PlayerRegistry;
import com.evofun.gameservice.shared.security.jwt.JwtUser;
import com.evofun.gameservice.feature.game.api.websocket.connection.WsClient;
import com.evofun.gameservice.feature.game.api.websocket.connection.WsClientRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WsPlayerConnectionService {//mb rename and move somewhere...
    private static final Logger logger = LoggerFactory.getLogger(WsPlayerConnectionService.class);

    private final WsClientRegistry clientHolder;
    private final PlayerRegistry playerRegistry;

    public WsPlayerConnectionService(WsClientRegistry clientHolder, PlayerRegistry playerRegistry) {
        this.clientHolder = clientHolder;
        this.playerRegistry = playerRegistry;
    }

    public Player processLogin(WsClient wsClient, JwtUser jwtUser) {
        Player player = playerRegistry.findPlayerById(jwtUser.getUserId());

        if (player != null) {
            clientHolder.reconnectClient(wsClient, wsClient.getSession(), jwtUser.getUserId());
            logger.info("{} ({}) was reconnected successfully", jwtUser.getNickname(), wsClient.getPlayerUUID());
            return player;
        } else {
            Player newPlayer = new Player();
            newPlayer.setUserId(jwtUser.getUserId());
            newPlayer.setNickname(jwtUser.getNickname());
            playerRegistry.addPlayer(newPlayer);
            clientHolder.promoteToAuthenticated(wsClient, newPlayer.getUserId());
            logger.info("Login successful for user {} ({})", newPlayer.getNickname(), newPlayer.getUserId());

            return newPlayer;
        }
    }
}
