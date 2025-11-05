package com.evofun.gameservice.feature.game.api.dto.response;

import com.evofun.gameservice.feature.game.domain.model.enums.GameDecision;
import com.evofun.gameservice.feature.game.domain.model.enums.IRoundResult;
import com.evofun.gameservice.feature.game.domain.model.Card;
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
public class SeatResponse {
    private UUID playerUUID;
    private int seatNumber;
    private int mainScore = 0;
    private List<Card> mainHand = new ArrayList<>();
    private BigDecimal currentBet = BigDecimal.ZERO;
    private GameDecision lastGameDecision = null;
    private IRoundResult roundResult = null;

    @JsonIgnore
    public SeatResponse(UUID playerUUID, int seatNumber) {
        this.playerUUID = playerUUID;
        this.seatNumber = seatNumber;
    }

    @JsonIgnore
    public SeatResponse(UUID playerUUID, int seatNumber, BigDecimal bet) {
        this.playerUUID = playerUUID;
        this.seatNumber = seatNumber;
        this.currentBet = bet;
    }
}
