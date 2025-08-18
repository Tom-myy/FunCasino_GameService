package com.evofun.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlayerInternalDto {
    private UUID userId;
    private String nickname;
    private BigDecimal balance;
    //internal:
    private BigDecimal balanceDelta;
    private List<SeatDto> seatsDto = new ArrayList<>();
    private boolean inTheGame = false;
}
