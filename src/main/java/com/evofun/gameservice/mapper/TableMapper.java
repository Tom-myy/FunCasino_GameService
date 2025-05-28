package com.evofun.gameservice.mapper;

import com.evofun.gameservice.model.TableModel;
import com.evofun.gameservice.dto.TableDto;

public class TableMapper {
    public static TableDto toDto(TableModel tableModel) {
        TableDto dto = new TableDto();

        dto.setSeatsDto(SeatMapper.toDtoList(tableModel.getSeatModels()));
        dto.setGameSeatsDto(SeatMapper.toDtoList(tableModel.getGameSeatModels()));
        dto.setPlayerNickNames(tableModel.getPlayerNickNames());
        dto.setDealerModel(tableModel.getDealerModel());
        dto.setGame(tableModel.isGame());
        dto.setPlayerCount(tableModel.getPlayerCount());

        return dto;
    }
}
