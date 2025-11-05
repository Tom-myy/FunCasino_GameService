package com.evofun.gameservice.feature.result.application.model;

import com.evofun.gameservice.feature.game.domain.model.Card;
import com.evofun.gameservice.feature.game.domain.model.enums.GameDecision;
import com.evofun.gameservice.feature.game.domain.model.enums.IRoundResult;
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
    private int seatNumber;
    private int mainScore;
    private List<Card> mainHand;
    private BigDecimal currentBet;
    private GameDecision lastGameDecision;
    private IRoundResult roundResult;
}