package com.nexora.network.whatif;

import com.nexora.multiplayer.engine.StateElementMetadata;
import com.nexora.multiplayer.engine.StatePriority;
import com.nexora.network.candidate.ActionType;
import com.nexora.network.candidate.CandidateAction;
import com.nexora.network.consistency.ConsistencyRiskAssessment;
import com.nexora.network.consistency.RiskLevel;
import com.nexora.network.prediction.NetworkPredictionResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Phase 13 (part 2) — Virtual What-If Evaluation + Consistency/Resource-Cost Evaluation.
 * In accordance with Patent Stage 204/205 & Claims 3, 5:
 *
 * Evaluates candidate communication-control strategies using an isolated discrete-event
 * queue simulator (VirtualQueueSimulator) under predicted future conditions without
 * touching or mutating the live session.
 *
 * Produces:
 *  1. Hard Feasibility Determination (Stage A Gate): Rejects candidates violating non-negotiable
 *     invariants for CRITICAL state elements.
 *  2. Multi-Objective Resource Cost (Stage B):
 *     J(a) = consistencyCost + networkCost + processingCost + controlOverhead
 */
@Component
public class WhatIfEvaluator {

    private record ActionProfile(double directResolution, double indirectRelief,
                                   double networkCostMultiplier, double processingCost, double controlOverheadBase) { }

    private static final Map<ActionType, ActionProfile> PROFILES = Map.of(
            ActionType.NO_ACTION, new ActionProfile(0, 0, 1.0, 0, 0),
            ActionType.REDUCE_LOW_PRIORITY_RATE, new ActionProfile(0.3, 0.2, 0.85, 1, 0.5),
            ActionType.AGGREGATE_UPDATES, new ActionProfile(0.4, 0.15, 0.70, 3, 0.5),
            ActionType.ENABLE_DEAD_RECKONING, new ActionProfile(0.85, 0.05, 0.60, 5, 0.5),
            ActionType.PRIORITIZE_CRITICAL, new ActionProfile(0.7, 0.0, 1.05, 2, 0.5)
    );

    private static double priorityWeight(StatePriority p) {
        return switch (p) {
            case CRITICAL -> 5.0;
            case HIGH -> 3.0;
            case MODERATE -> 2.0;
            case LOW -> 1.0;
            case VERY_LOW -> 0.5;
        };
    }

    public WhatIfResult evaluate(CandidateAction candidate, List<ConsistencyRiskAssessment> allAssessments,
                                  NetworkPredictionResult prediction) {
        ActionProfile profile = PROFILES.get(candidate.actionType);

        // 1. Run Discrete-Event Queue Simulation (Patent Stage 204)
        List<StateElementMetadata> elements = allAssessments.stream().map(a -> a.element).toList();
        VirtualQueueSimulator.QueueSimulationResult simResult =
                VirtualQueueSimulator.simulate(candidate, elements, prediction);

        double consistencyCost = 0;
        boolean feasible = true;
        String infeasibilityReason = "FEASIBLE";

        // 2. Consistency Cost Calculation factoring simulated queuing delay
        for (ConsistencyRiskAssessment a : allAssessments) {
            if (a.risk != RiskLevel.HIGH) continue;

            // Combine analytical overage with simulated queue staleness
            double simulatedStaleness = simResult.simulatedElementStalenessMs().getOrDefault(
                    a.element.getElementName(), a.predictedStalenessMs + simResult.averageQueuingDelayMs());

            double overage = priorityWeight(a.element.getPriority()) *
                    (Math.max(0, simulatedStaleness - a.element.getMaxStalenessMs())
                            + Math.max(0, a.predictedDivergenceEstimate - a.element.getMaxDivergence())) / 100.0;

            boolean targeted = candidate.targetElementNames.contains(a.element.getElementName());
            double remaining = overage * (1 - (targeted ? profile.directResolution() : profile.indirectRelief()));
            consistencyCost += remaining;

            // Hard Invariant Feasibility Check (Patent Stage 205 & Claim 5)
            if (a.element.getPriority() == StatePriority.CRITICAL) {
                if (a.element.getMaxStalenessMs() > 0 && simulatedStaleness > 3.0 * a.element.getMaxStalenessMs()) {
                    if (candidate.actionType != ActionType.NO_ACTION && !targeted) {
                        feasible = false;
                        infeasibilityReason = String.format("Critical state element '%s' simulated staleness (%.0fms) exceeds safety ceiling (%dms)",
                                a.element.getElementName(), simulatedStaleness, 3 * a.element.getMaxStalenessMs());
                    }
                }
            }
        }

        // 3. Network and Resource Costs derived from simulated queue metrics
        double baseThroughput = Math.max(1, prediction.predictedThroughput);
        // Effective network cost reflects simulated queue drain pressure
        double queuePressureFactor = 1.0 + (simResult.peakQueueDepth() / 50.0);
        double networkCost = baseThroughput * profile.networkCostMultiplier() * queuePressureFactor;
        double processingCost = profile.processingCost();
        double controlOverhead = candidate.actionType == ActionType.NO_ACTION
                ? 0
                : profile.controlOverheadBase() + 0.2 * candidate.targetElementNames.size();

        return new WhatIfResult(candidate, consistencyCost, networkCost, processingCost, controlOverhead,
                feasible, infeasibilityReason);
    }

    public List<WhatIfResult> evaluateAll(List<CandidateAction> candidates,
                                            List<ConsistencyRiskAssessment> allAssessments,
                                            NetworkPredictionResult prediction) {
        return candidates.stream().map(c -> evaluate(c, allAssessments, prediction)).toList();
    }
}
