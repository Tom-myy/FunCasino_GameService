package com.evofun.gameservice.feature.game.domain.model;

import com.evofun.gameservice.feature.game.api.websocket.exception.GameSystemException;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class Table {
    private static final Logger logger = LoggerFactory.getLogger(Table.class);
    @Getter
    private List<Player> players = new LinkedList<>();
    @Getter
    private List<Seat> seats = new ArrayList<>();
    @Getter
    private List<Seat> gameSeats = new ArrayList<>();
    @Setter
    @Getter
    private Dealer dealer = null;
    @Getter
    @Setter
    private boolean isGame = false;
    @Getter
    @Setter
    private int playerCount = 0;
    @Getter
    @Setter
    private Seat turnOfSeat = null;
    @Getter
    private Map<UUID, String> playerNickNames = new HashMap<>();//<userId, playerNickName>

    public void addPlayerNickName(Player player) {
        if (playerNickNames.containsKey(player.getUserId())) {
            return;
        }

        playerNickNames.put(player.getUserId(), player.getNickname());
    }

    public void removePlayerNickName(Player player) {
        if (!playerNickNames.containsKey(player.getUserId())) {
            logger.error("PlayerUUID doesn't exist in playerNickNames!");
            return;
        }

        playerNickNames.remove(player.getUserId());
    }

    public Table(List<Player> players) {
        this.players = players;
    }

    public boolean isSeatBusy(int seatNumber) {
        for (Seat seat : seats) {
            if (seat.getSeatNumber() == seatNumber)
                return true;
        }
        return false;
    }

    public void addSeat(Seat seat) {
        seats.add(seat);
    }

    public void removeSeat(Seat seat) {
        Integer index = null;
        for (Seat s : seats) {
            if (s.getSeatNumber() == seat.getSeatNumber()) {
                index = seats.indexOf(s);
                break;
            }
        }

        if (index != null) {
            seats.remove(index.intValue());
        } else {
            logger.error("Seat doesn't exist in seats!");
        }
    }

    public void removeSeatAtTheTableByKey(int key) {
        int index = -1;

        for (Seat seat : seats) {
            if (seat.getSeatNumber() == key) {
                index = seats.indexOf(seat);
            }
        }

        if (index != -1) {
            seats.remove(index);
        } else System.err.println("There is no such seat with key " + key);
    }

    public List<Seat> getCalculatedGameSeats() {
        //TODO мб сделать, чтобы тут ничего не изменялось, а просто передавались места
        List<Seat> calculatedGameSeats = new CopyOnWriteArrayList<>();
        for (Seat seat : seats) {
            if (seat.getCurrentBet().compareTo(BigDecimal.ZERO) > 0) {
                calculatedGameSeats.add(seat);
                seat.setInTheGame(true);
            }
        }
        calculatedGameSeats.sort(Comparator.comparing(Seat::getSeatNumber));

        return calculatedGameSeats;
    }

    public List<Seat> getAndSetGameSeats() {
        gameSeats = getCalculatedGameSeats();

        return gameSeats;
    }

    public List<Player> markAndGetPlayersInGame() {
        Map<UUID, Player> playerMap = players.stream()
                .collect(Collectors.toMap(Player::getUserId, p -> p));

        List<Player> playersInGame = new ArrayList<>();

        for (Seat seat : gameSeats) {
            Player player = playerMap.get(seat.getPlayerId());
            if (player != null) {
                player.setInTheGame(true);
                if (!playersInGame.contains(player)) {
                    playersInGame.add(player);
                }
            }
        }

        return playersInGame;
    }

    public boolean isThereSeatWithBetForPlayer(UUID playerUUID) {
        for (Seat seat : seats) {
            if (seat.getPlayerId().equals(playerUUID) && seat.getCurrentBet().compareTo(BigDecimal.ZERO) > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean isThereSeatForPlayer(UUID playerUUID) {
        for (Seat seat : seats) {
            if (seat.getPlayerId().equals(playerUUID)) {
                return true;
            }
        }
        return false;
    }

    public void removePlayerSeats(Player player) {
        for (int i = 0; i < player.getSeats().size(); i++) {
            int index = -1;

            for (Seat seat : seats) {
                if (seat.getPlayerId().equals(player.getUserId())) {
                    index = seats.indexOf(seat);
                    break;
                }
            }

            if (index != -1) {
                seats.remove(index);
            } else System.err.println("There is no such seats");
        }
    }

    public Seat getSeatByNumber(int seatNumber) {
        for (Seat seat : seats) {
            if (seat.getSeatNumber() == seatNumber) {
                return seat;
            }
        }

        throw new GameSystemException("There is no such seat with number " + seatNumber);
    }
}
