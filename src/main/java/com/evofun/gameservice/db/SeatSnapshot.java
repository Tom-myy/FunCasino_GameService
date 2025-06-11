package com.evofun.gameservice.db;

import com.evofun.gameservice.game.GameDecision;
import com.evofun.gameservice.game.RoundResult;
import com.evofun.gameservice.model.CardModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
@AllArgsConstructor
public class SeatSnapshot {
    private UUID playerUUID;
    private int seatNumber; int mainScore;
    private List<CardModel> mainHand;
    private BigDecimal currentBet;
    private GameDecision lastGameDecision;
    private RoundResult roundResult;
}