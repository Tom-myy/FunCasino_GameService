package com.evofun.gameservice.feature.game.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Card {
    private String initial;
    private String suit;
    private int coefficient;

    public Card(String initial, String suit, int coefficient) {
        this.initial = initial;
        this.suit = suit;
        this.coefficient = coefficient;
    }
}
