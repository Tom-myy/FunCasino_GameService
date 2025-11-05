package com.evofun.gameservice.feature.game.api.dto.response;

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
public class PlayerResponse {
    private String nickname;
    private List<SeatResponse> seatsDto = new ArrayList<>();
    private boolean inTheGame = false;
}
