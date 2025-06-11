package com.evofun.gameservice.db;

import com.evofun.gameservice.game.RoundResult;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "seat_in_game")
public class SeatInGame {
    @Id
    @Column(name = "seat_in_game_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "game_session_id", updatable = false, nullable = false)
    private UUID gameSessionId;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "round_result", nullable = false)
    private String roundResult;

    @Column(name = "bet", nullable = false)
    private BigDecimal bet;

/*    @Column(name = "profit", nullable = false)
    private BigDecimal profit;*/

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public SeatInGame(UUID gameSessionId, int seatNumber, UUID userId, RoundResult roundResult, BigDecimal bet/*, BigDecimal profit*/) {
        this.gameSessionId = gameSessionId;
        this.seatNumber = seatNumber;
        this.userId = userId;
        this.roundResult = roundResult.toString();
        this.bet = bet;
//        this.profit = profit;
    }
}