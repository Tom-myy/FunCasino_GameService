package com.evofun.gameservice.model;

import com.evofun.gameservice.game.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SeatModel {
    @Getter @Setter
    private UUID playerUUID;
    @Getter
    private int seatNumber;
    @Getter
    private int mainScore = 0;//TODO think about changing score at the moment getting new card in hand...
    @Getter
    private List<CardModel> mainHand = new ArrayList<>();
    @Setter @Getter
    private BigDecimal currentBet = BigDecimal.valueOf(0);
    @Setter @Getter
    private GameDecision lastGameDecision = null;
    @Getter @Setter
    private GameResultStatus gameResultStatus = null;

    //model fields:
    private String aceScore = "0/0";
    private static final int MINIMUM_ACE_SUMMAND = 1;
    private boolean isAceUsed = false;

    public SeatModel(UUID playerUUID, int seatNumber, int mainScore, List<CardModel> mainHand, BigDecimal currentBet, GameDecision lastGameDecision, GameResultStatus gameResultStatus) {
        this.playerUUID = playerUUID;
        this.seatNumber = seatNumber;
        this.mainScore = mainScore;
        this.mainHand = mainHand;
        this.currentBet = currentBet;
        this.lastGameDecision = lastGameDecision;
        this.gameResultStatus = gameResultStatus;
    }

    public void fullSeatReset(){
        mainScore = 0;
        currentBet = BigDecimal.ZERO;
        mainHand = new ArrayList<>();
        lastGameDecision = null;
        gameResultStatus = null;
        isAceUsed = false;
    }

    public void calculateScore(CardModel cardModel) {
        mainHand.add(cardModel);

        int score = mainScore;

        if(cardModel.getInitial().equalsIgnoreCase("Ace")){
            if(isAceUsed)
                score += MINIMUM_ACE_SUMMAND;
            else
                score += cardModel.getCoefficient();
        } else
            score += cardModel.getCoefficient();

        boolean hasAce = mainHand.stream().anyMatch(cardFromHand -> cardFromHand.getInitial().equalsIgnoreCase("Ace"));

        if(score > 21 && hasAce && !isAceUsed) {
            score -= 10;
            isAceUsed = true;
        }

        mainScore = score;

        if(mainScore > 21)
            setGameResultStatus(GameResultStatus.TOO_MANY);
    }

    public void resetGameResultStatus() {
        gameResultStatus = GameResultStatus.PROGRESSING;
    }

    public boolean equalsExcludingCurrentBet(SeatModel seatModel) {
        if (seatModel == null) return false;

        return seatNumber == seatModel.seatNumber &&
                mainScore == seatModel.mainScore &&
                Objects.equals(playerUUID, seatModel.playerUUID) &&
                Objects.equals(lastGameDecision, seatModel.lastGameDecision) &&
                gameResultStatus == seatModel.gameResultStatus &&
                Objects.equals(mainHand, seatModel.mainHand) &&
                Objects.equals(aceScore, seatModel.aceScore);
    }

    public boolean equalsBySeatNumberAndUUID(SeatModel seatModel) {
        if (seatModel == null) return false;

        return seatNumber == seatModel.seatNumber &&
                mainScore == seatModel.mainScore &&
                Objects.equals(playerUUID, seatModel.playerUUID);
    }

    public void printMoneyInfo(){
        System.out.println("seat=" + seatNumber + ", bet=" + currentBet);
    }

    @Override
    public String toString() {
        return "Seat{" +
                "isAceUsed=" + isAceUsed +
                ", aceScore='" + aceScore + '\'' +
                ", gameResultStatus=" + gameResultStatus +
                ", lastGameDecision=" + lastGameDecision +
                ", mainHand=" + mainHand +
                ", currentBet=" + currentBet +
                ", mainScore=" + mainScore +
                ", seatNumber=" + seatNumber +
                ", userUUID=" + playerUUID +
                '}';
    }



/* Mb will be user later...
    @Getter
    @Setter
    private GameDecision currentGameDecision = null;
    @Getter
    @Setter
    private boolean isBJ = false;
    @Getter
    @Setter
    private boolean isEnsured = false;

    public Seat() {
    }
    public Seat(UUID userUUID, int seatNumber) {
        this.userUUID = userUUID;
        this.seatNumber = seatNumber;
    }
    @JsonIgnore
    public Seat getSeat(){
        return this;
    }
    public void changeMainScore(int score) {
        this.mainScore += score;
    }
    public void resetScore() {
        this.mainScore = 0;
    }
    public Card getCurrentCardInHandByIndex(int i) {//TODO prevent array out of range
        return mainHand.get(i);
    }
    public void addOneCardInHand(Card card) {
        mainHand.add(card);
    }
    public void resetCardsInHand() {
        mainHand = new ArrayList<>();
    }
    public String getTwoAceScore() {
        return aceScore;
    }
    public void resetAceScore() {
        aceScore = "0/0";
    }
*/

}