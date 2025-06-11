package com.evofun.gameservice.db;

import com.evofun.gameservice.game.PlayerModel;
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
