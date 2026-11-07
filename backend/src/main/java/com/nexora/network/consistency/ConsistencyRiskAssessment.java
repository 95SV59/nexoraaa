package com.nexora.network.consistency;

import com.nexora.multiplayer.engine.StateElementMetadata;

/** One state element's predicted risk of violating its own consistency constraints,
 *  under the currently predicted future network condition. */
public class ConsistencyRiskAssessment {
    public final StateElementMetadata element;
    public final RiskLevel risk;
    public final String reason;
    public final double predictedStalenessMs;
    public final double predictedDivergenceEstimate;

    public ConsistencyRiskAssessment(StateElementMetadata element, RiskLevel risk, String reason,
                                      double predictedStalenessMs, double predictedDivergenceEstimate) {
        this.element = element;
        this.risk = risk;
        this.reason = reason;
        this.predictedStalenessMs = predictedStalenessMs;
        this.predictedDivergenceEstimate = predictedDivergenceEstimate;
    }
}
