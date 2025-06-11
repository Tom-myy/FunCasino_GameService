package com.evofun.gameservice.db;

import org.springframework.stereotype.Service;

@Service
public class GameSessionService {
    private final GameSessionRepo gameSessionRepo;

    public GameSessionService(GameSessionRepo gameSessionRepo) {
        this.gameSessionRepo = gameSessionRepo;
    }

    public GameSession saveGame(GameSession gameSession) {
        return gameSessionRepo.save(gameSession);
    }
}
