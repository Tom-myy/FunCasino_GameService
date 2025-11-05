package com.evofun.gameservice.feature.game.api.dto.response;

import com.evofun.gameservice.feature.game.domain.model.enums.RoundResult;
import com.evofun.gameservice.feature.game.domain.model.Card;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DealerResponse {
    private String nickName;
    private int score;
    private List<Card> hand;
    private RoundResult roundResult;
    private boolean isThereHiddenCard;
}
