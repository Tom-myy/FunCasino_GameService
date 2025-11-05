package com.evofun.gameservice.feature.result.application;

import com.evofun.gameservice.feature.result.infrastructure.persistence.entity.PlayerResultEntity;
import com.evofun.gameservice.feature.result.infrastructure.persistence.repository.PlayerResultRepo;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PlayerResultUseCase {
    private final PlayerResultRepo playerResultRepo;

    public PlayerResultUseCase(PlayerResultRepo playerResultRepo) {
        this.playerResultRepo = playerResultRepo;
    }

    public List<PlayerResultEntity> savePlayerResult(List<PlayerResultEntity> playerResultEntityList) {
        return playerResultRepo.saveAll(playerResultEntityList);
    }
}