package com.evofun.gameservice.feature.result.infrastructure.persistence.repository;

import com.evofun.gameservice.feature.result.infrastructure.persistence.entity.PlayerResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PlayerResultRepo extends JpaRepository<PlayerResultEntity, UUID> {}