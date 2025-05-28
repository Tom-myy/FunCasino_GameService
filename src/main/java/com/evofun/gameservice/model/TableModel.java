package com.evofun.gameservice.model;

import com.evofun.gameservice.game.PlayerModel;
import com.evofun.gameservice.websocket.exception.GameSystemException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class TableModel {

    @JsonIgnore
    private static final Logger logger = LoggerFactory.getLogger(TableModel.class);

    @JsonIgnore
    @Getter
    private List<PlayerModel> players = new LinkedList<>();//for money management

    //Common fields:
    @Getter
    private List<SeatModel> seatModels = new ArrayList<>();
    @Getter
    private List<SeatModel> gameSeatModels = new ArrayList<>();
    @Setter
    @Getter
    private DealerModel dealerModel = null;
    @Getter
    @Setter
    private boolean isGame = false;
    @Getter
    @Setter
    private int playerCount = 0;
    @Getter
    @Setter
    private UUID turnOfPlayerId = null;
/*    @Getter
    @Setter
    private EGamePhaseForInterface gamePhase = null;*/

    @Getter
    private Map<UUID, String> playerNickNames = new HashMap<>();//<userUUID, playerNickName>

    @JsonIgnore
    public void addPlayerNickName(PlayerModel player) {
        if (playerNickNames.containsKey(player.getPlayerUUID())) {
//            logger.error("PlayerUUID already exists in playerNickNames!");
            return;
        }

        playerNickNames.put(player.getPlayerUUID(), player.getUserModel().getNickname());
    }

    @JsonIgnore
    public void removePlayerNickName(PlayerModel player) {
        if (!playerNickNames.containsKey(player.getPlayerUUID())) {
            logger.error("PlayerUUID doesn't exist in playerNickNames!");
            return;
        }

        playerNickNames.remove(player.getPlayerUUID());
    }

    @JsonIgnore
    public TableModel(List<PlayerModel> players/*, PlayersBroadcastCallback callback*//*, MessageSender messageSender*/) {
        this.players = players;
//        this.playersBroadcastCallback = callback;
//        this.messageSender = messageSender;
    }

    //Common
    public boolean isSeatBusy(int seatNumber) {
        for (SeatModel seatModel : seatModels) {
            if (seatModel.getSeatNumber() == seatNumber)
                return true;
        }
        return false;
    }


    @JsonIgnore
    public void addSeat(SeatModel seatModel) {
        seatModels.add(seatModel);
    }

    @JsonIgnore
    public void removeSeat(SeatModel seatModel) {
        Integer index = null;
        for (SeatModel s : seatModels) {
            if(s.getSeatNumber() == seatModel.getSeatNumber()) {
                index = seatModels.indexOf(s);
                break;
            }
        }

        if(index != null) {
            seatModels.remove(index.intValue());
        } else {
            logger.error("Seat doesn't exist in seats!");
        }
    }

    @JsonIgnore
    public void removeSeatAtTheTableByKey(int key) {
        int index = -1;

        for (SeatModel seatModel : seatModels) {
            if (seatModel.getSeatNumber() == key) {
                index = seatModels.indexOf(seatModel);
            }
        }

        if (index != -1) {
            seatModels.remove(index);
        } else System.err.println("There is no such seat with key " + key);
    }

/*    @JsonIgnore
    public boolean isThereGameSeat() {
        List<Seat> gameSeats = new ArrayList<>();
        for (Seat seat : seats) {
//            if (seat.getCurrentBet() > 0)
            if (seat.getCurrentBet().compareTo(BigDecimal.ZERO) > 0)
                gameSeats.add(seat);
        }

        return !gameSeats.isEmpty();
    }*/

    @JsonIgnore
    public List<SeatModel> getCalculatedGameSeats() {
        List<SeatModel> calculatedGameSeatModels = new CopyOnWriteArrayList<>();
        for (SeatModel seatModel : seatModels) {
            if (seatModel.getCurrentBet().compareTo(BigDecimal.ZERO) > 0) {
                calculatedGameSeatModels.add(seatModel);
            }
        }
        calculatedGameSeatModels.sort(Comparator.comparing(SeatModel::getSeatNumber));

        return calculatedGameSeatModels;
    }

    @JsonIgnore
    public List<SeatModel> getAndSetGameSeats() {
        gameSeatModels = getCalculatedGameSeats();

        return gameSeatModels;
    }
    /*    @JsonIgnore
        public List<Seat> getAndSetGameSeats() {
            gameSeats = new CopyOnWriteArrayList<>();
            for (Seat seat : seats) {
                if (seat.getCurrentBet().compareTo(BigDecimal.ZERO) > 0) {
                    gameSeats.add(seat);//TODO mb change it to GameSeatsCollection
                }
            }
            gameSeats.sort(Comparator.comparing(Seat::getSeatNumber));

            return gameSeats;
        }*/


    @JsonIgnore
    public boolean isThereSeatWithBetForPlayer(UUID playerUUID) {
        for (SeatModel seatModel : seatModels) {
//            if (seat.getUserUUID().equals(userUUID) && seat.getCurrentBet() > 0) {
            if (seatModel.getPlayerUUID().equals(playerUUID) && seatModel.getCurrentBet().compareTo(BigDecimal.ZERO) > 0) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public boolean isThereSeatForPlayer(UUID playerUUID) {
        for (SeatModel seatModel : seatModels) {
            if (seatModel.getPlayerUUID().equals(playerUUID)) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public void removePlayerSeats(PlayerModel player) {
        for (int i = 0; i < player.getSeatModels().size(); i++) {
            int index = -1;

            for (SeatModel seatModel : seatModels) {
                if (seatModel.getPlayerUUID().equals(player.getPlayerUUID())) {
                    index = seatModels.indexOf(seatModel);
                    break;
                }
            }

            if (index != -1) {
                seatModels.remove(index);
            } else System.err.println("There is no such seats");
        }
    }
    public SeatModel getSeatByNumber(int seatNumber) {
        for (SeatModel seatModel : seatModels) {
            if (seatModel.getSeatNumber() == seatNumber) {
                return seatModel;
            }
        }

        throw new GameSystemException("There is no such seat with number " + seatNumber);
    }
}

