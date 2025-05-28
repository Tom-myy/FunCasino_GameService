package com.evofun.gameservice.websocket.handler.service;

import com.evofun.gameservice.dto.UserInternalDto;
import com.evofun.gameservice.game.PlayerModel;
import com.evofun.gameservice.game.PlayerRegistry;
import com.evofun.gameservice.mapper.UserInternalMapper;
import com.evofun.gameservice.model.UserModel;
import com.evofun.gameservice.websocket.connection.WsClient;
import com.evofun.gameservice.websocket.connection.WsClientRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WsPlayerConnectionService {//mb rename and move somewhere...
    private static final Logger logger = LoggerFactory.getLogger(WsPlayerConnectionService.class);

    private final WsClientRegistry clientHolder;
    private final PlayerRegistry playerRegistry;

    public WsPlayerConnectionService(WsClientRegistry clientHolder, PlayerRegistry playerRegistry) {
        this.clientHolder = clientHolder;
        this.playerRegistry = playerRegistry;
    }

    public PlayerModel processLogin(WsClient wsClient, UserModel userModel) {
        UUID playerUUID = userModel.getUserUUID();

        PlayerModel player = playerRegistry.findPlayerByUUID(playerUUID);
        if (player != null) {
            clientHolder.reconnectClient(wsClient, wsClient.getSession(), playerUUID);
            logger.info("{} ({}) was reconnected successfully", userModel.getNickname(), wsClient.getPlayerUUID());
            return player;
        } else {
            PlayerModel newPlayer = new PlayerModel();
            newPlayer.setUserModel(userModel);
            playerRegistry.addPlayer(newPlayer);
            clientHolder.promoteToAuthenticated(wsClient, newPlayer.getPlayerUUID());
            logger.info("Login successful for user {} ({})", newPlayer.getUserModel().getNickname(), newPlayer.getPlayerUUID());

            return newPlayer;
        }
    }
}
