package com.evofun.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlayerInternalDto {
    private UserInternalDto userInternalDto;
    private List<SeatDto> seatsDto = new ArrayList<>();
    private boolean inTheGame = false;
}
