package com.evofun.gameservice.feature.game.application.service;

import com.evofun.gameservice.GamePhaseUI;
import com.evofun.gameservice.feature.game.api.dto.response.SeatResponse;
import com.evofun.gameservice.feature.game.api.dto.response.TableResponse;
import com.evofun.gameservice.feature.game.api.websocket.exception.GameSystemException;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessage;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessageSenderImpl;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessageType;
import com.evofun.gameservice.feature.game.domain.model.Player;
import com.evofun.gameservice.feature.game.domain.model.Seat;
import com.evofun.gameservice.feature.game.domain.model.Table;
import com.evofun.gameservice.feature.game.infrastructure.mapper.TableMapper;
import com.evofun.gameservice.feature.game.utils.PlayerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class TableService {
    private static final Logger logger = LoggerFactory.getLogger(TableService.class);

    private final Table table;
    private final WsMessageSenderImpl messageSenderImpl;

    public TableService(WsMessageSenderImpl messageSenderImpl, PlayerRegistry playerRegistry) {
        this.messageSenderImpl = messageSenderImpl;
        this.table = new Table(playerRegistry.getPlayers());
    }

    public TableResponse getTableDto() {
        return TableMapper.toDto(table);
    }

    Table getTable() {
        ///package-private - only for 'service' package, exactly - for GameService
        return table;
    }

    public boolean isTableReadyToStartGame() {
        List<Seat> gameSeats = new ArrayList<>();

        for (Seat seat : table.getSeats()) {
            if (seat.getCurrentBet().compareTo(BigDecimal.ZERO) > 0)
                gameSeats.add(seat);
        }

        return !gameSeats.isEmpty();
    }

    public void addSeat(Seat seat) throws GameSystemException {
        if (isSeatBusy(seat)) {
            throw new GameSystemException("Seat is already taken");
        }
        table.addSeat(seat);
    }

    public List<Seat> getSeats() {
        return table.getSeats();
    }

    public void removeSeat(Seat seat) throws GameSystemException {
        if (!isSeatBusy(seat)) {
            throw new GameSystemException("Passed seat does not exist at the table");
        }
        table.removeSeat(seat);
    }

    public boolean isSeatBusy(Seat seat) {
        return table.isSeatBusy(seat.getSeatNumber());
    }

    public void sendPhaseUpdateToPlayer(SeatResponse seat) {
        if (table.isThereSeatWithBetForPlayer(seat.getPlayerUUID())) {//TODO think over it - it doesnt work properly
            messageSenderImpl.sendToClient(seat.getPlayerUUID(), new WsMessage<>(GamePhaseUI.READY_TO_GAME, WsMessageType.E_GAME_STATUS_FOR_INTERFACE));
        } else if (table.isThereSeatForPlayer(seat.getPlayerUUID())) {
            messageSenderImpl.sendToClient(seat.getPlayerUUID(), new WsMessage<>(GamePhaseUI.PLACING_BETS, WsMessageType.E_GAME_STATUS_FOR_INTERFACE));
        } else
            messageSenderImpl.sendToClient(seat.getPlayerUUID(), new WsMessage<>(GamePhaseUI.EMPTY_TABLE, WsMessageType.E_GAME_STATUS_FOR_INTERFACE));
    }

    public void replaceSeatAndUpdateBetAtTheTable(Seat seatForBetUpdating) throws GameSystemException {
        List<Seat> seats = table.getSeats();

        for (int i = 0; i < seats.size(); i++) {
            if (seats.get(i).equalsExcludingCurrentBet(seatForBetUpdating)) {
                seats.set(i, seatForBetUpdating);
                return;
            }
        }

        throw new GameSystemException("Seat wasn't changed. No matching seat found at the table");
    }

    public void doubleDownBet() {
        Seat seat = table.getTurnOfSeat();

        seat.setCurrentBet(seat.getCurrentBet().multiply(BigDecimal.TWO));
    }

    public List<Seat> getCalculatedGameSeats() {
        return table.getCalculatedGameSeats();
    }

    public List<Player> getPlayersWhoAreInGame() {
        return null;
    }

    public void addPlayerNickName(Player player) {
        table.addPlayerNickName(player);
    }

    public void removePlayerSeats(Player player) {
        table.removePlayerSeats(player);
    }

    public boolean isSeatExists(int seatNumber) {
        for (Seat seat : table.getSeats()) {
            if (seat.getSeatNumber() == seatNumber)
                return true;
        }
        return false;
    }

    public boolean isSeatOwnedByPlayer(UUID playerUUID, int seatNumber) {
        Seat seat = table.getSeatByNumber(seatNumber);

        return seat.getPlayerId().equals(playerUUID);
    }

    public void setPlayerCount(int playerCount) {
        table.setPlayerCount(playerCount);
    }

    public Seat getTurnOfSeat() {
        return table.getTurnOfSeat();
    }
}