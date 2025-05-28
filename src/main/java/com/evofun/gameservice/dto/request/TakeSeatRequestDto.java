package com.evofun.gameservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TakeSeatRequestDto {
    @Min(1)
    @Max(7)
    private int seatNumber;
}
