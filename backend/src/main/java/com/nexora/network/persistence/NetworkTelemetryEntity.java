package com.nexora.network.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/** Maps to network_telemetry (docs/DATABASE_DESIGN.md). One row per aggregated
 *  session-level NetworkSample (TelemetryCollector) — player_id stays null since we
 *  aggregate across players rather than tracking a per-player row today. */
@Entity
@Table(name = "network_telemetry")
public class NetworkTelemetryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "player_id")
    private Long playerId; // null: session-level aggregate

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "rtt_ms")
    private Double rttMs;

    @Column(name = "jitter_ms")
    private Double jitterMs;

    @Column(name = "packet_loss_pct")
    private Double packetLossPct;

    @Column(name = "packet_rate")
    private Double packetRate;

    protected NetworkTelemetryEntity() { }

    public NetworkTelemetryEntity(Long sessionId, double rttMs, double jitterMs,
                                   double packetLossPct, double packetRate) {
        this.sessionId = sessionId;
        this.recordedAt = Instant.now();
        this.rttMs = rttMs;
        this.jitterMs = jitterMs;
        this.packetLossPct = packetLossPct;
        this.packetRate = packetRate;
    }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public Instant getRecordedAt() { return recordedAt; }
    public Double getRttMs() { return rttMs; }
    public Double getJitterMs() { return jitterMs; }
    public Double getPacketLossPct() { return packetLossPct; }
    public Double getPacketRate() { return packetRate; }
}
