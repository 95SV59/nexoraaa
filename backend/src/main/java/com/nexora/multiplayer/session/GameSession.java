package com.nexora.multiplayer.session;

import jakarta.persistence.*;
import java.time.Instant;

/** One played match. Maps to game_session (docs/DATABASE_DESIGN.md). controllerMode
 *  distinguishes a Network Laboratory baseline run from a predictive-controller run
 *  once Phase 16/20 (experiments) land; defaults to PROPOSED for normal play. */
@Entity
@Table(name = "game_session")
public class GameSession {

    public enum ControllerMode { BASELINE, PROPOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "experiment_id")
    private Long experimentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "controller_mode", nullable = false)
    private ControllerMode controllerMode = ControllerMode.PROPOSED;

    protected GameSession() { }

    public GameSession(Long roomId) {
        this.roomId = roomId;
    }

    public Long getId() { return id; }
    public Long getRoomId() { return roomId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void markEnded() { this.endedAt = Instant.now(); }
    public Long getExperimentId() { return experimentId; }
    public void setExperimentId(Long experimentId) { this.experimentId = experimentId; }
    public ControllerMode getControllerMode() { return controllerMode; }
    public void setControllerMode(ControllerMode controllerMode) { this.controllerMode = controllerMode; }
}
