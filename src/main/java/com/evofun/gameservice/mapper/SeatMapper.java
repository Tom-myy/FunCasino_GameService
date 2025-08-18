package com.evofun.gameservice.mapper;

import com.evofun.gameservice.model.SeatModel;
import com.evofun.gameservice.dto.SeatDto;

import java.util.List;

public class SeatMapper {
    public static SeatModel toModel(SeatDto dto) {
        return new SeatModel(
                dto.getPlayerUUID(),
                dto.getSeatNumber(),
                dto.getMainScore(),
                dto.getMainHand(),
                dto.getCurrentBet(),
                dto.getLastGameDecision(),
                dto.getRoundResult()
        );
    }

    public static SeatDto toDto(SeatModel seatModel) {
        SeatDto dto = new SeatDto();

        dto.setPlayerUUID(seatModel.getPlayerId());
        dto.setSeatNumber(seatModel.getSeatNumber());
        dto.setMainScore(seatModel.getMainScore());
        dto.setMainHand(seatModel.getMainHand());
        dto.setCurrentBet(seatModel.getCurrentBet());
        dto.setLastGameDecision(seatModel.getLastGameDecision());
        dto.setRoundResult(seatModel.getRoundResult());

        return dto;
    }

    public static List<SeatDto> toDtoList(List<SeatModel> seatModelList) {
        return seatModelList.stream()
                .map(SeatMapper::toDto)
                .toList();
    }
}
