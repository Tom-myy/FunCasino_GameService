package com.evofun.gameservice.feature.game.infrastructure.mapper;

import com.evofun.gameservice.feature.game.domain.model.Seat;
import com.evofun.gameservice.feature.game.api.dto.response.SeatResponse;
import java.util.List;

public class SeatMapper {
    public static Seat toModel(SeatResponse dto) {
        return new Seat(
                dto.getPlayerUUID(),
                dto.getSeatNumber(),
                dto.getMainScore(),
                dto.getMainHand(),
                dto.getCurrentBet(),
                dto.getLastGameDecision(),
                dto.getRoundResult()
        );
    }

    public static SeatResponse toDto(Seat seat) {
        SeatResponse dto = new SeatResponse();

        dto.setPlayerUUID(seat.getPlayerId());
        dto.setSeatNumber(seat.getSeatNumber());
        dto.setMainScore(seat.getMainScore());
        dto.setMainHand(seat.getMainHand());
        dto.setCurrentBet(seat.getCurrentBet());
        dto.setLastGameDecision(seat.getLastGameDecision());
        dto.setRoundResult(seat.getRoundResult());

        return dto;
    }

    public static List<SeatResponse> toDtoList(List<Seat> seatList) {
        return seatList.stream()
                .map(SeatMapper::toDto)
                .toList();
    }
}
