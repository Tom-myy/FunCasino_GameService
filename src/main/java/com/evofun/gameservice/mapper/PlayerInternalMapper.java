package com.evofun.gameservice.mapper;

import com.evofun.gameservice.dto.PlayerInternalDto;
import com.evofun.gameservice.game.PlayerModel;

public class PlayerInternalMapper {
    public static PlayerInternalDto toPlayerInternalDto(PlayerModel playerModel) {
        PlayerInternalDto playerInternalDto = new PlayerInternalDto();

        playerInternalDto.setUserId(playerModel.getUserId());
        playerInternalDto.setNickname(playerModel.getNickname());
        playerInternalDto.setGameProfit(playerModel.getGameProfit());
        playerInternalDto.setSeatsDto(SeatMapper.toDtoList(playerModel.getSeatModels()));
        playerInternalDto.setInTheGame(playerModel.isInTheGame());

        return playerInternalDto;
    }
}
