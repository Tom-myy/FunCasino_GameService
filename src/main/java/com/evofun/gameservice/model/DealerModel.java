package com.evofun.gameservice.model;

import com.evofun.gameservice.game.GameResultStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class DealerModel {//TODO dto
    @Getter
    private final String nickName = "Dealer";
    @Getter
    private int score = 0;
    @Getter
    private List<CardModel> hand = new ArrayList<>();
    @Getter
    @Setter
    private GameResultStatus gameResultStatus;
    @Getter
    @Setter
    private boolean isThereHiddenCard = false;

    //model fields
    private CardModel hiddenCardModel = null;
    private static final int MINIMUM_ACE_SUMMAND = 1;
    private boolean isAceUsed = false;

    public CardModel getHiddenCardModel() {
        isThereHiddenCard = false;
        CardModel hidden = hiddenCardModel;
        hiddenCardModel = null;
        return hidden;
    }

    public void setHiddenCardModel(CardModel hiddenCardModel) {
        this.hiddenCardModel = hiddenCardModel;
        isThereHiddenCard = true;
    }

    public CardModel getCurrentCardInHandByIndex(int i) {//TODO prevent array out of range
        return hand.get(i);
    }

    public DealerModel() {}

/*    public void resetGameResultStatus(){
        gameResultStatus = null;//changed from PROGRESSING to null
    }*/

/*    public void changeScore(int score) {
        this.score += score;
    }*/

/*    public void resetScore() {
        this.score = 0;
    }*/

/*    public void addOneCardInHand(Card card) {
        hand.add(card);
    }*/

/*    public void resetCardsInHand() {
        hand = new ArrayList<>();
    }*/

    public void fullSeatReset(){
        score = 0;
        hand = new ArrayList<>();
        gameResultStatus = null;
        isAceUsed = false;
    }

    public void calculateScore(CardModel cardModel) {
        hand.add(cardModel);

        int tmpScore = score;

        if(cardModel.getInitial().equalsIgnoreCase("Ace")){
            if(isAceUsed)
                tmpScore += MINIMUM_ACE_SUMMAND;
            else
                tmpScore += cardModel.getCoefficient();
        } else
            tmpScore += cardModel.getCoefficient();

        boolean hasAce = hand.stream().anyMatch(cardFromHand -> cardFromHand.getInitial().equalsIgnoreCase("Ace"));

        if(tmpScore > 21 && hasAce && !isAceUsed) {
            tmpScore -= 10;
            isAceUsed = true;
        }

        score = tmpScore;

        if(score > 21)
            setGameResultStatus(GameResultStatus.TOO_MANY);
    }
}