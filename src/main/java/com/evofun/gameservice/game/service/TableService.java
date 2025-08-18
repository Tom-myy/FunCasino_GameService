package com.evofun.gameservice.game.service;

import com.evofun.gameservice.game.GamePhaseUI;
import com.evofun.gameservice.game.PlayerModel;
import com.evofun.gameservice.game.PlayerRegistry;
import com.evofun.gameservice.mapper.TableMapper;
import com.evofun.gameservice.model.SeatModel;
import com.evofun.gameservice.model.TableModel;
import com.evofun.gameservice.dto.SeatDto;
import com.evofun.gameservice.dto.TableDto;
import com.evofun.gameservice.websocket.exception.GameSystemException;
import com.evofun.gameservice.websocket.message.WsMessage;
import com.evofun.gameservice.websocket.message.WsMessageSenderImpl;
import com.evofun.gameservice.websocket.message.WsMessageType;
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

    private final TableModel tableModel;
    private final WsMessageSenderImpl messageSenderImpl;

    public TableService(WsMessageSenderImpl messageSenderImpl, PlayerRegistry playerRegistry) {
        this.messageSenderImpl = messageSenderImpl;
        this.tableModel = new TableModel(playerRegistry.getPlayerModels());
    }

    public TableDto getTableDto() {
        return TableMapper.toDto(tableModel);
    }

    TableModel getTable() {
        //package-private - only for 'service' package, exactly - for GameService
        return tableModel;
    }

    public boolean isTableReadyToStartGame() {
        List<SeatModel> gameSeatModels = new ArrayList<>();

        for (SeatModel seatModel : tableModel.getSeatModels()) {
            if (seatModel.getCurrentBet().compareTo(BigDecimal.ZERO) > 0)
                gameSeatModels.add(seatModel);
        }

        return !gameSeatModels.isEmpty();
    }

    public void addSeat(SeatModel seatModel) throws GameSystemException {
        if (isSeatBusy(seatModel)) {
            throw new GameSystemException("Seat is already taken");
        }
        tableModel.addSeat(seatModel);
    }

    public List<SeatModel> getSeats() {
        return tableModel.getSeatModels();
    }
    public List<PlayerModel> getPlayers() {
        return tableModel.getPlayers();
    }

    public void removeSeat(SeatModel seatModel) throws GameSystemException {
        if (!isSeatBusy(seatModel)) {
            throw new GameSystemException("Passed seat does not exist at the table");
        }
        tableModel.removeSeat(seatModel);
    }

    public boolean isSeatBusy(SeatModel seatModel) {
        return tableModel.isSeatBusy(seatModel.getSeatNumber());
    }

    public void sendPhaseUpdateToPlayer(SeatDto seat) {
        if (tableModel.isThereSeatWithBetForPlayer(seat.getPlayerUUID())) {//TODO think over it - it doesnt work properly
            messageSenderImpl.sendToClient(seat.getPlayerUUID(), new WsMessage<>(GamePhaseUI.READY_TO_GAME, WsMessageType.E_GAME_STATUS_FOR_INTERFACE));
        } else if (tableModel.isThereSeatForPlayer(seat.getPlayerUUID())) {
            messageSenderImpl.sendToClient(seat.getPlayerUUID(), new WsMessage<>(GamePhaseUI.PLACING_BETS, WsMessageType.E_GAME_STATUS_FOR_INTERFACE));
        } else
            messageSenderImpl.sendToClient(seat.getPlayerUUID(), new WsMessage<>(GamePhaseUI.EMPTY_TABLE, WsMessageType.E_GAME_STATUS_FOR_INTERFACE));
    }


    public void replaceSeatAndUpdateBetAtTheTable(SeatModel seatModelForBetUpdating) throws GameSystemException {
        List<SeatModel> seatModels = tableModel.getSeatModels();

        for (int i = 0; i < seatModels.size(); i++) {
            if (seatModels.get(i).equalsExcludingCurrentBet(seatModelForBetUpdating)) {
                seatModels.set(i, seatModelForBetUpdating);
                return;
            }
        }

        throw new GameSystemException("Seat wasn't changed. No matching seat found at the table");
    }

    //TODO it's mot correct to leave it here
    public void doubleDownBet(){//TODO mb to do smth here
        SeatModel seatModel = tableModel.getTurnOfSeat();


    }

    public List<SeatModel> getCalculatedGameSeats() {
        return tableModel.getCalculatedGameSeats();
    }

    public List<PlayerModel> getPlayersWhoAreInGame() {
        return null;
    }

    public void addPlayerNickName(PlayerModel player) {
        tableModel.addPlayerNickName(player);
    }

    public void removePlayerSeats(PlayerModel player) {
        tableModel.removePlayerSeats(player);
    }

    public boolean isSeatExists(int seatNumber) {
        for (SeatModel seatModel : tableModel.getSeatModels()) {
            if(seatModel.getSeatNumber() == seatNumber)
                return true;
        }
        return false;
    }

/*    public boolean isSeatOwnedByPlayer(UUID userId) {
        for (Seat seat : getTable().getSeats()) {
            if (userId.equals(seat.getUserId()))
                return true;
        }
        return false;
    }*/

    public boolean isSeatOwnedByPlayer(UUID playerUUID, int seatNumber) {
        SeatModel seatModel = tableModel.getSeatByNumber(seatNumber);

        return seatModel.getPlayerId().equals(playerUUID);
    }

    public void setPlayerCount(int playerCount) {
        tableModel.setPlayerCount(playerCount);
    }

    public SeatModel getTurnOfSeat() {
        return tableModel.getTurnOfSeat();
    }
}
