package com.evofun.gameservice.game;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Getter
@Component
public class PlayerRegistry {
    private final List<PlayerModel> playerModels = new LinkedList<>();//TODO change to Map<UUID, Player> players = new ConcurrentHashMap<>() for productivity

    public void addPlayer(PlayerModel playerModel) {
        playerModels.add(playerModel);
    }

    public PlayerModel findPlayerByUUID(UUID playerUUID) {
        for (PlayerModel playerModel : playerModels) {
            if (playerModel.getPlayerUUID().equals(playerUUID)) {
                return playerModel;
            }
        }
        return null;
    }

    public List<PlayerModel> getPlayersWhoAreInGame() {
        List<PlayerModel> result = new LinkedList<>();
        for (PlayerModel p : playerModels) {
            if (p.isInTheGame())
                result.add(p);
        }

        return result;
    }

    public void removePlayerByUUID(UUID uuid) {
        PlayerModel playerModel = findPlayerByUUID(uuid);
        if (playerModel != null)
            playerModels.remove(playerModel);
    }
}