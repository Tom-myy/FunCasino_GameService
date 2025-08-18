package com.evofun.gameservice.websocket.handler;

import com.evofun.gameservice.MoneyServiceClient;
import com.evofun.gameservice.db.UserGameBalanceDto;
import com.evofun.gameservice.exception.NotEnoughBalanceException;
import com.evofun.gameservice.game.PlayerModel;
import com.evofun.gameservice.game.PlayerService;
import com.evofun.gameservice.mapper.PlayerPublicMapper;
import com.evofun.gameservice.mapper.SeatMapper;
import com.evofun.gameservice.model.SeatModel;
import com.evofun.gameservice.game.service.GameService;
import com.evofun.gameservice.game.service.TableService;
import com.evofun.gameservice.websocket.validation.ValidationService;
import com.evofun.gameservice.dto.request.TakeSeatRequestDto;
import com.evofun.gameservice.dto.request.UpdateBetRequestDto;
import com.evofun.gameservice.dto.request.leaveSeatRequestDto;
import com.evofun.gameservice.dto.SeatDto;
import com.evofun.gameservice.websocket.exception.GameValidationException;
import com.evofun.gameservice.websocket.message.WsMessage;
import com.evofun.gameservice.websocket.message.WsMessageSenderImpl;
import com.evofun.gameservice.websocket.message.WsMessageType;
import com.evofun.gameservice.websocket.connection.WsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class WsSeatHandler {
    //TODO create validation methode for seat checking...
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
        TakeSeatRequestDto dto = objectMapper.convertValue(wsMessage.getMessage(), TakeSeatRequestDto.class);

        validationService.validateRequestDto(dto);

        if (tableService.isSeatExists(dto.getSeatNumber()))
            throw new GameValidationException("This seat is already busy at the table.", "Player attempted to occupy a seat that was already occupied.");

        SeatDto seatDto = new SeatDto(wsClient.getPlayerUUID(), dto.getSeatNumber());
        SeatModel seatModel = SeatMapper.toModel(seatDto);

        tableService.addSeat(seatModel);
        playerService.addSeat(seatModel);
//        messageSenderImpl.sendToClient(seat.getUserId(), new WsMessage<>(PlayerMapper.toDto(player)/*player*/, WsMessageType.PLAYER_DATA));//TODO is this necessary?

        messageSenderImpl.broadcast(new WsMessage<>(SeatMapper.toDtoList(tableService.getSeats()), WsMessageType.SEATS));

        tableService.sendPhaseUpdateToPlayer(seatDto);
    }

    public void handleLeaveSeat(WsMessage<?> wsMessage, WsClient wsClient) {
        leaveSeatRequestDto dto = objectMapper.convertValue(wsMessage.getMessage(), leaveSeatRequestDto.class);

        validationService.validateRequestDto(dto);

        if (!tableService.isSeatExists(dto.getSeatNumber())) {
            throw new GameValidationException("There's no such seat at the table to leave.", "Player tried to leave seat that is not taken yet.");
        }

        if (!tableService.isSeatOwnedByPlayer(wsClient.getPlayerUUID(), dto.getSeatNumber())) {
            throw new GameValidationException("This is not your seat.", "Player tried to leave seat that does not own it.");
        }

        SeatDto seatDto = new SeatDto(wsClient.getPlayerUUID(), dto.getSeatNumber());
        SeatModel seatModel = SeatMapper.toModel(seatDto);

        PlayerModel playerModel = playerService.removeSeatAndRefund(seatModel);
        tableService.removeSeat(seatModel);
        //TODO not sure that i need to send 'PLAYER_DATA' to player.. if need - mb send it in 'handleTakeSeat':
        messageSenderImpl.sendToClient(seatModel.getPlayerId(), new WsMessage<>(PlayerPublicMapper.toPlayerPublicDto(playerModel)/*player*/, WsMessageType.PLAYER_DATA));//TODO playerDTO

        tableService.sendPhaseUpdateToPlayer(seatDto);

        messageSenderImpl.broadcast(new WsMessage<>(SeatMapper.toDtoList(tableService.getSeats()), WsMessageType.SEATS));
    }

    public void handleUpdateSeatBet(WsMessage<?> wsMessage, WsClient wsClient) {
        UpdateBetRequestDto dto = objectMapper.convertValue(wsMessage.getMessage(), UpdateBetRequestDto.class);

        validationService.validateRequestDto(dto);

        if (!tableService.isSeatExists(dto.getSeatNumber()))
            throw new GameValidationException("There's no such seat at the table.", "Player tried to update bet for seat that is not taken yet.");

        if (!tableService.isSeatOwnedByPlayer(wsClient.getPlayerUUID(), dto.getSeatNumber()))
            throw new GameValidationException("This is not your seat.", "Player tried to update bet for seat that does not own it.");

        if(!moneyServiceClient.reserveMoneyForBet(wsClient.getPlayerUUID(), dto.getBet()))
            throw new NotEnoughBalanceException("Player doesn't have enough money for his bet. ",
                    "You don't have enough money for this bet.");

        SeatDto seatDto = new SeatDto(wsClient.getPlayerUUID(), dto.getSeatNumber(), dto.getBet());
        SeatModel seatModel = SeatMapper.toModel(seatDto);

        tableService.replaceSeatAndUpdateBetAtTheTable(seatModel);
        PlayerModel playerModel = playerService.replaceSeatAndUpdateBetInPlayer(seatModel);
        //TODO not sure that i need to send 'PLAYER_DATA' to player.. if need - mb send it in 'handleTakeSeat':
        messageSenderImpl.sendToClient(seatModel.getPlayerId(), new WsMessage<>(PlayerPublicMapper.toPlayerPublicDto(playerModel)/*player*/, WsMessageType.PLAYER_DATA));//TODO playerDTO

        gameService.tryStartBettingTime();

        messageSenderImpl.broadcast(new WsMessage<>(SeatMapper.toDtoList(tableService.getSeats()), WsMessageType.SEATS));

        tableService.sendPhaseUpdateToPlayer(seatDto);
    }
}