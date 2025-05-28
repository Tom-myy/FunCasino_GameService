package com.evofun.gameservice.model;

import java.util.Collections;
import java.util.List;

public class DeckShufflerModel {

    public static void myShuffle(List<CardModel> deck) {
//        ColorPrint.printPurple("It's time to shuffle the deck");
        System.out.println("It's time to shuffle the deck");
        Collections.shuffle(deck);
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
//        ColorPrint.printPurple("The deck was shuffled");
        System.out.println("The deck was shuffled");
    }
}
