package com.nexora.network.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/** Maps to network_prediction (docs/DATABASE_DESIGN.md). */
@Entity
@Table(name = "network_prediction")
public class NetworkPredictionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "predicted_at", nullable = false)
    private Instant predictedAt;

    @Column(name = "predicted_rtt_ms")
    private Double predictedRttMs;

    @Column(name = "predicted_jitter_ms")
    private Double predictedJitterMs;

    @Column(name = "predicted_loss_pct")
    private Double predictedLossPct;

    @Column(name = "predicted_throughput_kbps")
    private Double predictedThroughputKbps; // stand-in: our packet-rate-based estimate, not true kbps

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "horizon_ms")
    private Integer horizonMs;

    protected NetworkPredictionEntity() { }

    public NetworkPredictionEntity(Long sessionId, double predictedRttMs, double predictedJitterMs,
                                    double predictedLossPct, double predictedThroughputKbps,
                                    double confidence, long horizonMs) {
        this.sessionId = sessionId;
        this.predictedAt = Instant.now();
        this.predictedRttMs = predictedRttMs;
        this.predictedJitterMs = predictedJitterMs;
        this.predictedLossPct = predictedLossPct;
        this.predictedThroughputKbps = predictedThroughputKbps;
        this.confidence = confidence;
        this.horizonMs = (int) horizonMs;
    }

    public Long getId() { return id; }
    public Double getConfidence() { return confidence; }
    public Double getPredictedRttMs() { return predictedRttMs; }
}
