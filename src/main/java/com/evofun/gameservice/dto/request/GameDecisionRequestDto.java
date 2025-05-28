package com.evofun.gameservice.dto.request;

import com.evofun.gameservice.game.GameDecision;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GameDecisionRequestDto {
    @NotNull
    private GameDecision gameDecision;

}
