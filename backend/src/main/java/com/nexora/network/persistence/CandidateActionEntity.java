package com.nexora.network.persistence;

import jakarta.persistence.*;

/** Maps to candidate_action (docs/DATABASE_DESIGN.md). parameters is stored as a JSON string. */
@Entity
@Table(name = "candidate_action")
public class CandidateActionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prediction_id", nullable = false)
    private Long predictionId;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Lob
    @Column(name = "parameters")
    private String parametersJson;

    protected CandidateActionEntity() { }

    public CandidateActionEntity(Long predictionId, String actionType, String parametersJson) {
        this.predictionId = predictionId;
        this.actionType = actionType;
        this.parametersJson = parametersJson;
    }

    public Long getId() { return id; }
    public String getActionType() { return actionType; }
}
