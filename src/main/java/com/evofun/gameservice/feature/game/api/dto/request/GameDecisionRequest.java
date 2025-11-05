package com.evofun.gameservice.feature.game.api.dto.request;

import com.evofun.gameservice.feature.game.domain.model.enums.GameDecision;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GameDecisionRequest {
    @NotNull
    private GameDecision gameDecision;

}
