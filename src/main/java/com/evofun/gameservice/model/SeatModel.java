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
    private UUID playerId;
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
    private IRoundResult roundResult = null;
    @Getter @Setter
    boolean inTheGame = false;

    //model fields:
    private String aceScore = "0/0";
    private static final int MINIMUM_ACE_SUMMAND = 1;
    private boolean isAceUsed = false;

    public SeatModel(UUID playerId, int seatNumber, int mainScore, List<CardModel> mainHand, BigDecimal currentBet, GameDecision lastGameDecision, IRoundResult roundResult) {
        this.playerId = playerId;
        this.seatNumber = seatNumber;
        this.mainScore = mainScore;
        this.mainHand = mainHand;
        this.currentBet = currentBet;
        this.lastGameDecision = lastGameDecision;
        this.roundResult = roundResult;
    }

    public void restartBeforeGame(){
        mainScore = 0;
        mainHand = new ArrayList<>();
        lastGameDecision = null;
        roundResult = null;
        isAceUsed = false;
//        inTheGame = true;//it seems that Game marks them already
    }

    public void restartAfterGame(){
        mainScore = 0;
        currentBet = BigDecimal.ZERO;
        mainHand = new ArrayList<>();
        lastGameDecision = null;
        roundResult = null;
        isAceUsed = false;
        inTheGame = false;
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
            setRoundResult(FinalRoundResult.LOSE);
//            setGameResultStatus(GameResultStatus.TOO_MANY);
    }

    public void resetGameResultStatus() {
        roundResult = null;
    }

    public boolean equalsExcludingCurrentBet(SeatModel seatModel) {
        if (seatModel == null) return false;

        return seatNumber == seatModel.seatNumber &&
                mainScore == seatModel.mainScore &&
                Objects.equals(playerId, seatModel.playerId) &&
                Objects.equals(lastGameDecision, seatModel.lastGameDecision) &&
                roundResult == seatModel.roundResult &&
                Objects.equals(mainHand, seatModel.mainHand) &&
                Objects.equals(aceScore, seatModel.aceScore);
    }

    public boolean equalsBySeatNumberAndUUID(SeatModel seatModel) {
        if (seatModel == null) return false;

        return seatNumber == seatModel.seatNumber &&
                mainScore == seatModel.mainScore &&
                Objects.equals(playerId, seatModel.playerId);
    }

    public void printMoneyInfo(){
        System.out.println("seat=" + seatNumber + ", bet=" + currentBet);
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
    public Seat(UUID userId, int seatNumber) {
        this.userId = userId;
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