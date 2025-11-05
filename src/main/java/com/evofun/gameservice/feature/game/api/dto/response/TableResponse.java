package com.evofun.gameservice.feature.game.api.dto.response;

import com.evofun.gameservice.feature.game.domain.model.Dealer;
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
public class TableResponse {
    private List<SeatResponse> seatsDto;
    private List<SeatResponse> gameSeatsDto;
    private Dealer dealer = null;
    private boolean isGame = false;
    private int playerCount = 0;
    private Map<UUID, String> playerNickNames = new HashMap<>();//<userId, playerNickName>
}
