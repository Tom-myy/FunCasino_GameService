package com.evofun.gameservice.feature.game.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class leaveSeatRequest {
    @Min(1)
    @Max(7)
    private int seatNumber;
}
