package com.evofun.gameservice.db;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerInGameSessionService {
    private final SeatInGameRepo seatInGameRepo;

    public PlayerInGameSessionService(SeatInGameRepo seatInGameRepo) {
        this.seatInGameRepo = seatInGameRepo;
    }

    public List<GameSessionSeat> savePlayerInGameSession(List<GameSessionSeat> gameSessionSeatList) {
        return seatInGameRepo.saveAll(gameSessionSeatList);
    }
}
