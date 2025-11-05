package com.evofun.gameservice.feature.result.application;

import com.evofun.gameservice.feature.result.infrastructure.persistence.repository.GameResultRepo;
import com.evofun.gameservice.feature.result.infrastructure.persistence.entity.GameResultEntity;
import org.springframework.stereotype.Service;

@Service
public class GameResultUseCase {
    private final GameResultRepo gameResultRepo;

    public GameResultUseCase(GameResultRepo gameResultRepo) {
        this.gameResultRepo = gameResultRepo;
    }

    public GameResultEntity saveGameResult(GameResultEntity gameResultEntity) {
        return gameResultRepo.save(gameResultEntity);
    }
}