package com.evofun.gameservice.forGame;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserBalanceUpdateDto {
    private UUID userId;
    private BigDecimal balanceDelta;
}
