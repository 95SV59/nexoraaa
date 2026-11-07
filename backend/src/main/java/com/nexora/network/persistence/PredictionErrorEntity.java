package com.nexora.network.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/** Maps to prediction_error (docs/DATABASE_DESIGN.md). actual_divergence/actual_cost and
 *  their error columns are left null — FeedbackService (Phase 15) currently measures only
 *  RTT and packet loss against telemetry; measuring actual in-match state divergence would
 *  need instrumentation inside each MultiplayerGame to report it, which is a further
 *  increment (see docs/ARCHITECTURE.md §6 on the consistency model). */
@Entity
@Table(name = "prediction_error")
public class PredictionErrorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "decision_id", nullable = false, unique = true)
    private Long decisionId;

    @Column(name = "actual_rtt_ms")
    private Double actualRttMs;

    @Column(name = "actual_loss_pct")
    private Double actualLossPct;

    @Column(name = "rtt_error")
    private Double rttError;

    @Column(name = "loss_error")
    private Double lossError;

    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    protected PredictionErrorEntity() { }

    public PredictionErrorEntity(Long decisionId, double actualRttMs, double actualLossPct,
                                  double rttError, double lossError) {
        this.decisionId = decisionId;
        this.actualRttMs = actualRttMs;
        this.actualLossPct = actualLossPct;
        this.rttError = rttError;
        this.lossError = lossError;
        this.measuredAt = Instant.now();
    }

    public Long getId() { return id; }
    public Double getRttError() { return rttError; }
    public Double getLossError() { return lossError; }
}
