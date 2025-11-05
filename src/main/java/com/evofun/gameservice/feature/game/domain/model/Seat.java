package com.evofun.gameservice.feature.game.domain.model;

import com.evofun.gameservice.feature.game.domain.model.enums.FinalRoundResult;
import com.evofun.gameservice.feature.game.domain.model.enums.GameDecision;
import com.evofun.gameservice.feature.game.domain.model.enums.IRoundResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Seat {
    @Getter
    @Setter
    private UUID playerId;
    @Getter
    private int seatNumber;
    @Getter
    private int mainScore = 0;//TODO think about changing score at the moment getting a new card in hand...
    @Getter
    private List<Card> mainHand = new ArrayList<>();
    @Setter
    @Getter
    private BigDecimal currentBet = BigDecimal.valueOf(0);
    @Setter
    @Getter
    private GameDecision lastGameDecision = null;
    @Getter
    @Setter
    private IRoundResult roundResult = null;
    @Getter
    @Setter
    boolean inTheGame = false;

    private String aceScore = "0/0";
    private static final int MINIMUM_ACE_SUMMAND = 1;
    private boolean isAceUsed = false;

    public Seat(UUID playerId, int seatNumber, int mainScore, List<Card> mainHand, BigDecimal currentBet, GameDecision lastGameDecision, IRoundResult roundResult) {
        this.playerId = playerId;
        this.seatNumber = seatNumber;
        this.mainScore = mainScore;
        this.mainHand = mainHand;
        this.currentBet = currentBet;
        this.lastGameDecision = lastGameDecision;
        this.roundResult = roundResult;
    }

    public void restartBeforeGame() {
        mainScore = 0;
        mainHand = new ArrayList<>();
        lastGameDecision = null;
        roundResult = null;
        isAceUsed = false;
    }

    public void restartAfterGame() {
        mainScore = 0;
        currentBet = BigDecimal.ZERO;
        mainHand = new ArrayList<>();
        lastGameDecision = null;
        roundResult = null;
        isAceUsed = false;
        inTheGame = false;
    }

    public void calculateScore(Card card) {
        mainHand.add(card);

        int score = mainScore;

        if (card.getInitial().equalsIgnoreCase("Ace")) {
            if (isAceUsed)
                score += MINIMUM_ACE_SUMMAND;
            else
                score += card.getCoefficient();
        } else
            score += card.getCoefficient();

        boolean hasAce = mainHand.stream().anyMatch(cardFromHand -> cardFromHand.getInitial().equalsIgnoreCase("Ace"));

        if (score > 21 && hasAce && !isAceUsed) {
            score -= 10;
            isAceUsed = true;
        }

        mainScore = score;

        if (mainScore > 21)
            setRoundResult(FinalRoundResult.LOSE);
    }

    public void resetGameResultStatus() {
        roundResult = null;
    }

    public boolean equalsExcludingCurrentBet(Seat seat) {
        if (seat == null) return false;

        return seatNumber == seat.seatNumber &&
                mainScore == seat.mainScore &&
                Objects.equals(playerId, seat.playerId) &&
                Objects.equals(lastGameDecision, seat.lastGameDecision) &&
                roundResult == seat.roundResult &&
                Objects.equals(mainHand, seat.mainHand) &&
                Objects.equals(aceScore, seat.aceScore);
    }

    public boolean equalsBySeatNumberAndUUID(Seat seat) {
        if (seat == null) return false;

        return seatNumber == seat.seatNumber &&
                mainScore == seat.mainScore &&
                Objects.equals(playerId, seat.playerId);
    }

    @Override
    public String toString() {
        return "Seat{" +
                "isAceUsed=" + isAceUsed +
                ", aceScore='" + aceScore + '\'' +
                ", gameResultStatus=" + roundResult +
                ", lastGameDecision=" + lastGameDecision +
                ", mainHand=" + mainHand +
                ", currentBet=" + currentBet +
                ", mainScore=" + mainScore +
                ", seatNumber=" + seatNumber +
                ", userId=" + playerId +
                '}';
    }
}