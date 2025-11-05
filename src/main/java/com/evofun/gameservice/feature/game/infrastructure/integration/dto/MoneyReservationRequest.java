package com.evofun.gameservice.feature.game.infrastructure.integration.dto;

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
public class MoneyReservationRequest {
    private UUID userId;
    private BigDecimal amount;
}
