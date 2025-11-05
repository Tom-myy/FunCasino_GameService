package com.evofun.gameservice.feature.game.utils;

import com.evofun.gameservice.feature.game.domain.model.Player;
import lombok.Getter;
import org.springframework.stereotype.Component;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Getter
@Component
public class PlayerRegistry {
    private final List<Player> players = new LinkedList<>();//TODO change to Map<UUID, Player> playersInGameSession = new ConcurrentHashMap<>() for productivity

    public void addPlayer(Player player) {
        players.add(player);
    }

    public Player findPlayerById(UUID playerUUID) {
        for (Player player : players) {
            if (player.getUserId().equals(playerUUID)) {
                return player;
            }
        }
        return null;
    }

    public List<Player> getPlayersWhoAreInGame() {
        List<Player> result = new LinkedList<>();
        for (Player p : players) {
            if (p.isInTheGame())
                result.add(p);
        }

        return result;
    }

    public void removePlayerByUUID(UUID uuid) {
        Player player = findPlayerById(uuid);
        if (player != null)
            players.remove(player);
    }
}