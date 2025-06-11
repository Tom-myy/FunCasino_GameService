package com.evofun.gameservice.db;

import com.evofun.gameservice.model.UserModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PlayerSnapshot {
    private UserModel userModel;
    List<SeatSnapshot> seats;
    boolean inTheGame;
}
