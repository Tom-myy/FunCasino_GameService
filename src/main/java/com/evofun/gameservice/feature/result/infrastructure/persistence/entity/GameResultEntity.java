package com.evofun.gameservice.feature.result.infrastructure.persistence.entity;

import com.evofun.gameservice.feature.result.application.model.GameResultStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "game_sessions")
public class GameResultEntity {
    //TODO create new migration and change the table according to the new name
    @Id
    @Column(name = "game_session_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "dealer_score", nullable = false)
    private int dealerScore;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at", nullable = false)
    private OffsetDateTime finishedAt;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = OffsetDateTime.now();
        }
        if (finishedAt == null) {
            finishedAt = OffsetDateTime.now();
        }
    }

    public GameResultEntity(UUID id, int dealerScore, GameResultStatus status) {
        this.id = id;
        this.dealerScore = dealerScore;
        this.status = status.toString();
    }
}