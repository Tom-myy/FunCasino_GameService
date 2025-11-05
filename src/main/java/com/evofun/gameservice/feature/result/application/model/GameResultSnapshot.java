package com.evofun.gameservice.feature.result.application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class GameResultSnapshot {
    private List<PlayerSnapshot> playersInGameSession;
    private int dealerScore;
}
