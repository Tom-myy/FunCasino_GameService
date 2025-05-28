package com.evofun.gameservice.mapper;

import com.evofun.gameservice.dto.PlayerPublicDto;
import com.evofun.gameservice.game.PlayerModel;


public class PlayerPublicMapper {
    public static PlayerPublicDto toPlayerPublicDto(PlayerModel playerModel) {
        PlayerPublicDto playerPublicDto = new PlayerPublicDto();

        playerPublicDto.setUserPublicDto(UserPublicMapper.toPublicDto(playerModel.getUserModel()));
        playerPublicDto.setSeatsDto(SeatMapper.toDtoList(playerModel.getSeatModels()));
        playerPublicDto.setInTheGame(playerModel.isInTheGame());

        return playerPublicDto;
    }

/*    public static PlayerModel toPlayerModel(PlayerPublicDto playerPublicDto) {//TODO fuck.. and mb unneeded!
        PlayerModel playerModel = new PlayerModel();

        playerModel.setUserModel(UserPublicMapper.toUserModel(playerPublicDto.getUserPublicDto()));
        playerModel.setSeatModels(SeatMapper.toModelList(playerPublicDto.getSeatsDto()));
        playerModel.setInTheGame(playerPublicDto.isInTheGame());

        return playerModel;
    }*/



}
