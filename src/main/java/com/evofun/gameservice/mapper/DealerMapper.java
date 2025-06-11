package com.evofun.gameservice.mapper;

import com.evofun.gameservice.model.DealerModel;
import com.evofun.gameservice.dto.DealerDto;

public class DealerMapper {
    public static DealerDto toDto(DealerModel dealerModel) {
        DealerDto dto = new DealerDto();

        dto.setNickName(dealerModel.getNickName());
        dto.setScore(dealerModel.getScore());
        dto.setHand(dealerModel.getHand());
        dto.setRoundResult(dealerModel.getRoundResult());
        dto.setThereHiddenCard(dealerModel.isThereHiddenCard());

        return dto;
    }
}
