package com.evofun.gameservice.feature.game.domain.model;

import java.util.Collections;
import java.util.List;

public class Shuffler {

    public static void shuffle(List<Card> deck) {
        System.out.println("It's time to shuffle the deck");

        Collections.shuffle(deck);
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);

        }
        System.out.println("The deck was shuffled");
    }
}
