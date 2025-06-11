package com.evofun.gameservice.dto;

import com.evofun.gameservice.game.RoundResult;
import com.evofun.gameservice.model.CardModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DealerDto {
    private String nickName;
    private int score;
    private List<CardModel> hand;
    private RoundResult roundResult;
    private boolean isThereHiddenCard;
}
