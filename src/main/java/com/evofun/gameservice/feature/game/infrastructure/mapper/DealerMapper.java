package com.evofun.gameservice.feature.game.infrastructure.mapper;

import com.evofun.gameservice.feature.game.domain.model.Dealer;
import com.evofun.gameservice.feature.game.api.dto.response.DealerResponse;

public class DealerMapper {
    public static DealerResponse toDto(Dealer dealer) {
        DealerResponse dto = new DealerResponse();

        dto.setNickName(dealer.getNickName());
        dto.setScore(dealer.getScore());
        dto.setHand(dealer.getHand());
        dto.setRoundResult(dealer.getRoundResult());
        dto.setThereHiddenCard(dealer.isThereHiddenCard());

        return dto;
    }
}
