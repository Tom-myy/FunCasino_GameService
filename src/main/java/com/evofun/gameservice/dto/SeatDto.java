package com.evofun.gameservice.dto;

import com.evofun.gameservice.game.GameDecision;
import com.evofun.gameservice.game.GameResultStatus;
import com.evofun.gameservice.model.CardModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class SeatDto {
    private UUID playerUUID;
    private int seatNumber;
    private int mainScore = 0;
    private List<CardModel> mainHand = new ArrayList<>();
    private BigDecimal currentBet = BigDecimal.ZERO;
    private GameDecision lastGameDecision = null;
    private GameResultStatus gameResultStatus = null;

    @JsonIgnore
    public SeatDto(UUID playerUUID, int seatNumber) {
        this.playerUUID = playerUUID;
        this.seatNumber = seatNumber;
    }

    @JsonIgnore
    public SeatDto(UUID playerUUID, int seatNumber, BigDecimal bet) {
        this.playerUUID = playerUUID;
        this.seatNumber = seatNumber;
        this.currentBet = bet;
    }
}

