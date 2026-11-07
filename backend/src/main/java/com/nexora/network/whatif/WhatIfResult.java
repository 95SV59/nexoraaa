package com.nexora.network.whatif;

import com.nexora.network.candidate.CandidateAction;

/**
 * Maps to whatif_evaluation (docs/DATABASE_DESIGN.md).
 * In accordance with Patent Stage 205 / Claim 5:
 * Evaluates candidate-specific consistency cost, network cost, processing cost,
 * and control overhead, alongside an explicit hard-constraint feasibility gate.
 */
public class WhatIfResult {
    public final CandidateAction candidate;
    public final double consistencyCost;
    public final double networkCost;
    public final double processingCost;
    public final double controlOverhead;
    public final double totalScore;
    public final boolean feasible;
    public final String feasibilityReason;

    public WhatIfResult(CandidateAction candidate, double consistencyCost, double networkCost,
                         double processingCost, double controlOverhead) {
        this(candidate, consistencyCost, networkCost, processingCost, controlOverhead, true, "FEASIBLE");
    }

    public WhatIfResult(CandidateAction candidate, double consistencyCost, double networkCost,
                         double processingCost, double controlOverhead,
                         boolean feasible, String feasibilityReason) {
        this.candidate = candidate;
        this.consistencyCost = consistencyCost;
        this.networkCost = networkCost;
        this.processingCost = processingCost;
        this.controlOverhead = controlOverhead;
        this.feasible = feasible;
        this.feasibilityReason = feasibilityReason != null ? feasibilityReason : (feasible ? "FEASIBLE" : "INFEASIBLE");
        this.totalScore = consistencyCost + networkCost + processingCost + controlOverhead;
    }
}
