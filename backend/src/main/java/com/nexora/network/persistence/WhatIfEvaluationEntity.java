package com.nexora.network.persistence;

import jakarta.persistence.*;

/** Maps to whatif_evaluation (docs/DATABASE_DESIGN.md). predicted_consistency/predicted_cost
 *  are legacy summary columns alongside the detailed cost breakdown; we fill them from
 *  totalScore/consistencyCost since WhatIfEvaluator doesn't compute a separate figure for
 *  either — see docs/ARCHITECTURE.md §5 on the cost model being an experimental design point. */
@Entity
@Table(name = "whatif_evaluation")
public class WhatIfEvaluationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidate_id", nullable = false, unique = true)
    private Long candidateId;

    @Column(name = "predicted_consistency")
    private Double predictedConsistency;

    @Column(name = "predicted_cost")
    private Double predictedCost;

    @Column(name = "consistency_cost")
    private Double consistencyCost;

    @Column(name = "network_cost")
    private Double networkCost;

    @Column(name = "processing_cost")
    private Double processingCost;

    @Column(name = "control_overhead")
    private Double controlOverhead;

    @Column(name = "total_score")
    private Double totalScore;

    protected WhatIfEvaluationEntity() { }

    public WhatIfEvaluationEntity(Long candidateId, double consistencyCost, double networkCost,
                                   double processingCost, double controlOverhead, double totalScore) {
        this.candidateId = candidateId;
        this.predictedConsistency = consistencyCost; // see class javadoc
        this.predictedCost = totalScore;
        this.consistencyCost = consistencyCost;
        this.networkCost = networkCost;
        this.processingCost = processingCost;
        this.controlOverhead = controlOverhead;
        this.totalScore = totalScore;
    }

    public Long getId() { return id; }
    public Long getCandidateId() { return candidateId; }
    public Double getPredictedConsistency() { return predictedConsistency; }
    public Double getPredictedCost() { return predictedCost; }
    public Double getConsistencyCost() { return consistencyCost; }
    public Double getNetworkCost() { return networkCost; }
    public Double getProcessingCost() { return processingCost; }
    public Double getControlOverhead() { return controlOverhead; }
    public Double getTotalScore() { return totalScore; }
}
