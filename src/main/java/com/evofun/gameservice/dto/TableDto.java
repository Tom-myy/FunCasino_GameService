package com.evofun.gameservice.dto;

import com.evofun.gameservice.model.DealerModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class TableDto {
    private List<SeatDto> seatsDto;
    private List<SeatDto> gameSeatsDto;
    private DealerModel dealerModel = null;
    private boolean isGame = false;
    private int playerCount = 0;
    private Map<UUID, String> playerNickNames = new HashMap<>();//<userUUID, playerNickName>
}
