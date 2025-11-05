package com.evofun.gameservice.feature.game.infrastructure.mapper;

import com.evofun.gameservice.feature.game.api.dto.response.PlayerResponse;
import com.evofun.gameservice.feature.game.domain.model.Player;

public class PlayerMapper {
    public static PlayerResponse toResponse(Player player) {
        PlayerResponse playerResponse = new PlayerResponse();

        playerResponse.setNickname(player.getNickname());
        playerResponse.setSeatsDto(SeatMapper.toDtoList(player.getSeats()));
        playerResponse.setInTheGame(player.isInTheGame());

        return playerResponse;
    }
}