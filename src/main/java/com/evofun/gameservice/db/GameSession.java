package com.evofun.gameservice.db;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "game_sessions")
public class GameSession {
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
/*        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = GameSessionStatus.FINISHED;
        }*/
        if (startedAt == null) {
            startedAt = OffsetDateTime.now();
        }
        if (finishedAt == null) {
            finishedAt = OffsetDateTime.now();
        }
    }

    public GameSession(UUID id, int dealerScore, GameSessionStatus status/*, OffsetDateTime startedAt, OffsetDateTime finishedAt*/) {
        this.id = id;
        this.dealerScore = dealerScore;
        this.status = status.toString();
/*        this.startedAt = startedAt;
        this.finishedAt = finishedAt;*/
    }
}