package com.evofun.gameservice.feature.game.domain.model;

import com.evofun.gameservice.feature.game.domain.model.enums.CardSuit;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

public class SmallDeck {//TODO create huge deck (416 cards)
    private static final int COUNT_OF_INITIAL = 13;

    @Getter
    private List<Card> smallDeck = new ArrayList<>();//52 cards

    public SmallDeck() {
        createOneUsualDeck();
    }

    private void createOneUsualDeck() {
        CardSuit[] suits = CardSuit.values();
        for (CardSuit suit : suits) {
            String currentSuit = "";
            int coefficient = 0;

            switch(suit){
                case HEARTS: currentSuit = "Hearts";
                    break;

                case CLUBS: currentSuit = "Clubs";
                    break;

                case DIAMONDS: currentSuit = "Diamonds";
                    break;

                case SPADES: currentSuit = "Spades";
                    break;

            }

            for (int init = 2; init < COUNT_OF_INITIAL+2; ++init) {
                String currentInit = String.valueOf(init);

                if(init >= 2 && init <= 10){
                    coefficient = init;
                }
                if (init == 11) {
                    currentInit = "Jack";
                    coefficient = 10;
                }
                if (init == 12) {
                    currentInit = "Quin";
                    coefficient = 10;
                }
                if (init == 13) {
                    currentInit = "King";
                    coefficient = 10;
                }
                if (init == 14) {
                    currentInit = "Ace";
                    coefficient = 11;
                }
                smallDeck.add(new Card(currentInit, currentSuit, coefficient));
            }
        }
    }
}
