package com.evofun.gameservice.feature.result.infrastructure.persistence.repository;

import com.evofun.gameservice.feature.result.infrastructure.persistence.entity.GameResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface GameResultRepo extends JpaRepository<GameResultEntity, UUID> {}