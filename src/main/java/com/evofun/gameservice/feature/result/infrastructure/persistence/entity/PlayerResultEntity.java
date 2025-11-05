package com.evofun.gameservice.feature.result.infrastructure.persistence.entity;

import com.evofun.gameservice.feature.game.domain.model.enums.IRoundResult;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "game_session_seats")
public class PlayerResultEntity {
    //TODO create new migration and change the table according to the new name
    @Id
    @Column(name = "game_session_seat_id", updatable = false, nullable = false)
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

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public PlayerResultEntity(UUID gameSessionId, int seatNumber, UUID userId, IRoundResult roundResult, BigDecimal bet) {
        this.gameSessionId = gameSessionId;
        this.seatNumber = seatNumber;
        this.userId = userId;
        this.roundResult = roundResult.toString();
        this.bet = bet;
    }
}