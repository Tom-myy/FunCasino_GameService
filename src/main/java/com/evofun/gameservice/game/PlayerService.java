package com.evofun.gameservice.game;

import com.evofun.gameservice.mapper.PlayerPublicMapper;
import com.evofun.gameservice.model.SeatModel;
import com.evofun.gameservice.websocket.exception.GameSystemException;
import com.evofun.gameservice.websocket.message.WsMessage;
import com.evofun.gameservice.websocket.message.WsMessageSenderImpl;
import com.evofun.gameservice.websocket.message.WsMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PlayerService {
    private static final Logger logger = LoggerFactory.getLogger(PlayerService.class);
    private final PlayerRegistry playerRegistry;
    private final WsMessageSenderImpl messageSenderImpl;

    public PlayerService(PlayerRegistry playerRegistry, WsMessageSenderImpl messageSenderImpl) {
        this.playerRegistry = playerRegistry;
        this.messageSenderImpl = messageSenderImpl;
    }

    public void addSeat(SeatModel seatModel) throws GameSystemException {
        PlayerModel playerModel = playerRegistry.findPlayerById(seatModel.getPlayerId());
        if (playerModel == null) {
            throw new GameSystemException("Player not found in playerRegistry during seat adding (userId = " + seatModel.getPlayerId() + ")");
        }
        playerModel.addSeat(seatModel);
        messageSenderImpl.sendToClient(seatModel.getPlayerId(), new WsMessage<>(PlayerPublicMapper.toPlayerPublicDto(playerModel), WsMessageType.PLAYER_DATA));
    }

    public PlayerModel removeSeat(SeatModel seatModel) throws GameSystemException {
        //TODO запросить возрат средств с MoneyService
        PlayerModel playerModel = getPlayerByUUIDOrThrow(seatModel.getPlayerId());
        SeatModel seatModelOfPlayer = findSeatOfPlayerOrThrow(playerModel, seatModel);

        playerModel.getSeatModels().remove(seatModelOfPlayer);

        return playerModel;
    }

    private SeatModel findSeatOfPlayerOrThrow(PlayerModel playerModel, SeatModel seatModelRef) throws GameSystemException {
        return playerModel.getSeatModels().stream()
                .filter(s -> s.equalsBySeatNumberAndUUID(seatModelRef))
                .findFirst()
                .orElseThrow(() -> new GameSystemException("Seat not found in player's collection"));
    }

    public PlayerModel getPlayerByUUIDOrThrow(UUID playerUUID) throws GameSystemException {
        PlayerModel playerModel = playerRegistry.findPlayerById(playerUUID);
        if (playerModel == null) {
            throw new GameSystemException("Player not found in playerRegistry (userId = " + playerUUID + ")");
        }
        return playerModel;
    }

    public PlayerModel replaceSeatAndUpdateBetInPlayer(SeatModel seatModel) throws GameSystemException {//TODO not sure that it work correctly...
        PlayerModel playerModel = getPlayerByUUIDOrThrow(seatModel.getPlayerId());
        SeatModel oldSeatModel = findSeatOfPlayerOrThrow(playerModel, seatModel);

        int seatIndex = playerModel.getSeatModels().indexOf(oldSeatModel);
        if (seatIndex == -1) {
            throw new GameSystemException("Seat to replace not found in player's seat list (userId=" + playerModel.getUserId() + ")");
        }

        BigDecimal oldBet = oldSeatModel.getCurrentBet();
        BigDecimal newBet = seatModel.getCurrentBet();

        playerModel.getSeatModels().set(seatIndex, seatModel);

        return playerModel;
    }
}
