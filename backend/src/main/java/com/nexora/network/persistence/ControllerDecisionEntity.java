package com.nexora.network.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/** Maps to controller_decision (docs/DATABASE_DESIGN.md). */
@Entity
@Table(name = "controller_decision")
public class ControllerDecisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "prediction_id", nullable = false)
    private Long predictionId;

    @Column(name = "selected_candidate_id")
    private Long selectedCandidateId; // null only if persistence couldn't resolve it — see NetworkPersistenceService

    @Column(name = "confidence_gate_passed", nullable = false)
    private boolean confidenceGatePassed;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    protected ControllerDecisionEntity() { }

    public ControllerDecisionEntity(Long sessionId, Long predictionId, Long selectedCandidateId,
                                     boolean confidenceGatePassed) {
        this.sessionId = sessionId;
        this.predictionId = predictionId;
        this.selectedCandidateId = selectedCandidateId;
        this.confidenceGatePassed = confidenceGatePassed;
        this.appliedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getSelectedCandidateId() { return selectedCandidateId; }
    public boolean isConfidenceGatePassed() { return confidenceGatePassed; }
}
