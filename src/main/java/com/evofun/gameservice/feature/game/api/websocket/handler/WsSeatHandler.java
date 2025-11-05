package com.evofun.gameservice.feature.game.api.websocket.handler;

import com.evofun.gameservice.feature.game.api.dto.request.TakeSeatRequest;
import com.evofun.gameservice.feature.game.api.dto.request.UpdateBetRequest;
import com.evofun.gameservice.feature.game.api.dto.request.leaveSeatRequest;
import com.evofun.gameservice.feature.game.api.dto.response.SeatResponse;
import com.evofun.gameservice.feature.game.api.websocket.connection.WsClient;
import com.evofun.gameservice.feature.game.api.websocket.exception.GameValidationException;
import com.evofun.gameservice.feature.game.api.websocket.exception.NotEnoughBalanceException;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessage;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessageSenderImpl;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessageType;
import com.evofun.gameservice.feature.game.api.websocket.service.ValidationService;
import com.evofun.gameservice.feature.game.application.service.GameService;
import com.evofun.gameservice.feature.game.application.service.PlayerService;
import com.evofun.gameservice.feature.game.application.service.TableService;
import com.evofun.gameservice.feature.game.domain.model.Player;
import com.evofun.gameservice.feature.game.domain.model.Seat;
import com.evofun.gameservice.feature.game.infrastructure.integration.MoneyServiceClient;
import com.evofun.gameservice.feature.game.infrastructure.mapper.PlayerMapper;
import com.evofun.gameservice.feature.game.infrastructure.mapper.SeatMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class WsSeatHandler {
    //TODO add logging with info (etc.) level for successful actions
    private final ObjectMapper objectMapper;
    private final WsMessageSenderImpl messageSenderImpl;
    private final TableService tableService;
    private final PlayerService playerService;
    private final GameService gameService;
    private final ValidationService validationService;
    private final MoneyServiceClient moneyServiceClient;

    public WsSeatHandler(ObjectMapper objectMapper, WsMessageSenderImpl messageSenderImpl, TableService tableService, PlayerService playerService, GameService gameService, ValidationService validationService, MoneyServiceClient moneyServiceClient) {
        this.objectMapper = objectMapper;
        this.messageSenderImpl = messageSenderImpl;
        this.tableService = tableService;
        this.playerService = playerService;
        this.gameService = gameService;
        this.validationService = validationService;
        this.moneyServiceClient = moneyServiceClient;
    }

    public void handleTakeSeat(WsMessage<?> wsMessage, WsClient wsClient) {
        TakeSeatRequest dto = objectMapper.convertValue(wsMessage.getMessage(), TakeSeatRequest.class);

        validationService.validateRequest(dto);

        if (tableService.isSeatExists(dto.getSeatNumber()))
            throw new GameValidationException("This seat is already busy at the table.", "Player attempted to occupy a seat that was already occupied.");

        SeatResponse seatResponse = new SeatResponse(wsClient.getPlayerUUID(), dto.getSeatNumber());
        Seat seat = SeatMapper.toModel(seatResponse);

        tableService.addSeat(seat);
        playerService.addSeat(seat);

        messageSenderImpl.broadcast(new WsMessage<>(SeatMapper.toDtoList(tableService.getSeats()), WsMessageType.SEATS));

        tableService.sendPhaseUpdateToPlayer(seatResponse);
    }

    public void handleLeaveSeat(WsMessage<?> wsMessage, WsClient wsClient) {
        leaveSeatRequest dto = objectMapper.convertValue(wsMessage.getMessage(), leaveSeatRequest.class);

        validationService.validateRequest(dto);

        if (!tableService.isSeatExists(dto.getSeatNumber())) {
            throw new GameValidationException("There's no such seat at the table to leave.", "Player tried to leave seat that is not taken yet.");
        }

        if (!tableService.isSeatOwnedByPlayer(wsClient.getPlayerUUID(), dto.getSeatNumber())) {
            throw new GameValidationException("This is not your seat.", "Player tried to leave seat that does not own it.");
        }

        SeatResponse seatResponse = new SeatResponse(wsClient.getPlayerUUID(), dto.getSeatNumber());
        Seat seatModel = SeatMapper.toModel(seatResponse);

        Seat oldSeat = null;
        for (Seat seat : tableService.getSeats()) {
            if (seat.getSeatNumber() == dto.getSeatNumber()) {
                oldSeat = seat;
                break;
            }
        }

        if (!oldSeat.getCurrentBet().equals(BigDecimal.ZERO)) {
            moneyServiceClient.cancelBet(wsClient.getPlayerUUID(), oldSeat.getCurrentBet());
        }
        Player player = playerService.removeSeat(seatModel);
        tableService.removeSeat(seatModel);
        //TODO not sure that i need to send 'PLAYER_DATA' to player.. if need - mb send it in 'handleTakeSeat':
        messageSenderImpl.sendToClient(seatModel.getPlayerId(), new WsMessage<>(PlayerMapper.toResponse(player), WsMessageType.PLAYER_DATA));

        tableService.sendPhaseUpdateToPlayer(seatResponse);

        messageSenderImpl.broadcast(new WsMessage<>(SeatMapper.toDtoList(tableService.getSeats()), WsMessageType.SEATS));
    }

    public void handleUpdateSeatBet(WsMessage<?> wsMessage, WsClient wsClient) {
        UpdateBetRequest request = objectMapper.convertValue(wsMessage.getMessage(), UpdateBetRequest.class);

        validationService.validateRequest(request);

        if (!tableService.isSeatExists(request.getSeatNumber()))
            throw new GameValidationException("There's no such seat at the table.", "Player tried to update bet for seat that is not taken yet.");

        if (!tableService.isSeatOwnedByPlayer(wsClient.getPlayerUUID(), request.getSeatNumber()))
            throw new GameValidationException("This is not your seat.", "Player tried to update bet for seat that does not own it.");

        if (!moneyServiceClient.reserveMoneyForBet(wsClient.getPlayerUUID(), request.getBet()))
            throw new NotEnoughBalanceException("Player doesn't have enough money for his bet. ",
                    "You don't have enough money for this bet.");

        SeatResponse seatResponse = new SeatResponse(wsClient.getPlayerUUID(), request.getSeatNumber(), request.getBet());
        Seat seat = SeatMapper.toModel(seatResponse);

        tableService.replaceSeatAndUpdateBetAtTheTable(seat);
        Player player = playerService.replaceSeatAndUpdateBetInPlayer(seat);
        //TODO not sure that i need to send 'PLAYER_DATA' to player.. if need - mb send it in 'handleTakeSeat':
        messageSenderImpl.sendToClient(seat.getPlayerId(), new WsMessage<>(PlayerMapper.toResponse(player), WsMessageType.PLAYER_DATA));

        gameService.tryStartBettingTime();

        messageSenderImpl.broadcast(new WsMessage<>(SeatMapper.toDtoList(tableService.getSeats()), WsMessageType.SEATS));

        tableService.sendPhaseUpdateToPlayer(seatResponse);
    }
}