package com.evofun.gameservice.db;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class PlayerSnapshot {
    private UUID userId;
    private String nickname;
    private BigDecimal gameProfit;
    List<SeatSnapshot> seats;
    boolean inTheGame;
}
