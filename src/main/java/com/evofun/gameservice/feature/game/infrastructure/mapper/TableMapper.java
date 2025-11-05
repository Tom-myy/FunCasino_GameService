package com.evofun.gameservice.feature.game.infrastructure.mapper;

import com.evofun.gameservice.feature.game.domain.model.Table;
import com.evofun.gameservice.feature.game.api.dto.response.TableResponse;

public class TableMapper {
    public static TableResponse toDto(Table table) {
        TableResponse dto = new TableResponse();

        dto.setSeatsDto(SeatMapper.toDtoList(table.getSeats()));
        dto.setGameSeatsDto(SeatMapper.toDtoList(table.getGameSeats()));
        dto.setPlayerNickNames(table.getPlayerNickNames());
        dto.setDealer(table.getDealer());
        dto.setGame(table.isGame());
        dto.setPlayerCount(table.getPlayerCount());

        return dto;
    }
}
