package com.evofun.gameservice.feature.game.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateBetRequest {
    @Min(1)
    @Max(7)
    private int seatNumber;
    @NotNull
    @DecimalMin("1")
    @DecimalMax("10000")
    private BigDecimal bet;
}
