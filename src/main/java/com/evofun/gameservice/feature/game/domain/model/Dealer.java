package com.evofun.gameservice.feature.game.domain.model;

import com.evofun.gameservice.feature.game.domain.model.enums.RoundResult;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

public class Dealer {
    @Getter
    private final String nickName = "Dealer";
    @Getter
    private int score = 0;
    @Getter
    private List<Card> hand = new ArrayList<>();
    @Getter
    @Setter
    private RoundResult roundResult;
    @Getter
    @Setter
    private boolean isThereHiddenCard = false;
    private Card hiddenCard = null;
    private static final int MINIMUM_ACE_SUMMAND = 1;
    private boolean isAceUsed = false;

    public Card getHiddenCard() {
        isThereHiddenCard = false;
        Card hidden = hiddenCard;
        hiddenCard = null;
        return hidden;
    }

    public void setHiddenCard(Card hiddenCard) {
        this.hiddenCard = hiddenCard;
        isThereHiddenCard = true;
    }

    public Card getCurrentCardInHandByIndex(int i) {//TODO prevent array out of range
        return hand.get(i);
    }

    public void fullSeatReset(){
        score = 0;
        hand = new ArrayList<>();
        roundResult = null;
        isAceUsed = false;
    }

    public void calculateScore(Card card) {
        hand.add(card);

        int tmpScore = score;

        if(card.getInitial().equalsIgnoreCase("Ace")){
            if(isAceUsed)
                tmpScore += MINIMUM_ACE_SUMMAND;
            else
                tmpScore += card.getCoefficient();
        } else
            tmpScore += card.getCoefficient();

        boolean hasAce = hand.stream().anyMatch(cardFromHand -> cardFromHand.getInitial().equalsIgnoreCase("Ace"));

        if(tmpScore > 21 && hasAce && !isAceUsed) {
            tmpScore -= 10;
            isAceUsed = true;
        }

        score = tmpScore;

        if(score > 21)
            setRoundResult(RoundResult.LOSE);
    }
}