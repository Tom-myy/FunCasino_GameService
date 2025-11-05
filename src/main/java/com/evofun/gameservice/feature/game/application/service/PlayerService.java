package com.evofun.gameservice.feature.game.application.service;

import com.evofun.gameservice.feature.game.api.websocket.exception.GameSystemException;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessage;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessageSenderImpl;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessageType;
import com.evofun.gameservice.feature.game.domain.model.Player;
import com.evofun.gameservice.feature.game.domain.model.Seat;
import com.evofun.gameservice.feature.game.infrastructure.mapper.PlayerMapper;
import com.evofun.gameservice.feature.game.utils.PlayerRegistry;
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

    public void addSeat(Seat seat) throws GameSystemException {
        Player player = playerRegistry.findPlayerById(seat.getPlayerId());
        if (player == null) {
            throw new GameSystemException("Player not found in playerRegistry during seat adding (userId = " + seat.getPlayerId() + ")");
        }
        player.addSeat(seat);
        messageSenderImpl.sendToClient(seat.getPlayerId(), new WsMessage<>(PlayerMapper.toResponse(player), WsMessageType.PLAYER_DATA));
    }

    public Player removeSeat(Seat seat) throws GameSystemException {
        //TODO request a refund from money-service
        Player player = getPlayerByUUIDOrThrow(seat.getPlayerId());
        Seat seatOfPlayer = findSeatOfPlayerOrThrow(player, seat);

        player.getSeats().remove(seatOfPlayer);

        return player;
    }

    private Seat findSeatOfPlayerOrThrow(Player player, Seat seatRef) throws GameSystemException {
        return player.getSeats().stream()
                .filter(s -> s.equalsBySeatNumberAndUUID(seatRef))
                .findFirst()
                .orElseThrow(() -> new GameSystemException("Seat not found in player's collection"));
    }

    public Player getPlayerByUUIDOrThrow(UUID playerUUID) throws GameSystemException {
        Player player = playerRegistry.findPlayerById(playerUUID);
        if (player == null) {
            throw new GameSystemException("Player not found in playerRegistry (userId = " + playerUUID + ")");
        }
        return player;
    }

    public Player replaceSeatAndUpdateBetInPlayer(Seat seat) throws GameSystemException {//TODO not sure that it work correctly...
        Player player = getPlayerByUUIDOrThrow(seat.getPlayerId());
        Seat oldSeat = findSeatOfPlayerOrThrow(player, seat);

        int seatIndex = player.getSeats().indexOf(oldSeat);
        if (seatIndex == -1) {
            throw new GameSystemException("Seat to replace not found in player's seat list (userId=" + player.getUserId() + ")");
        }

        player.getSeats().set(seatIndex, seat);

        return player;
    }
}
