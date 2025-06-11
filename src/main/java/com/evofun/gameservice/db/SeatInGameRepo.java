package com.evofun.gameservice.db;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SeatInGameRepo extends JpaRepository<SeatInGame, UUID> {
}
