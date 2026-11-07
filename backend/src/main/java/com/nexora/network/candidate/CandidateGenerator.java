package com.nexora.network.candidate;

import com.nexora.multiplayer.engine.StatePriority;
import com.nexora.network.consistency.ConsistencyRiskAssessment;
import com.nexora.network.consistency.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Phase 13 (part 1) — Candidate Network-Control Generation. Turns the consistency
 * engine's per-element risk into a short list of concrete, differently-targeted
 * strategies for WhatIfEvaluator to score. NO_ACTION is always included as the
 * baseline every other candidate must beat.
 */
@Component
public class CandidateGenerator {

    /**
     * Actions the running system can genuinely apply today (see
     * NetworkControllerOrchestrator.mapActionToBroadcastThrottle). PRIORITIZE_CRITICAL is
     * deliberately absent: packet-priority reordering isn't implementable over STOMP's simple
     * broker, so proposing it made the what-if model credit the controller with fixing risk it
     * could not actually fix — and in simulation it won ~25% of cycles under UNSTABLE, flapping
     * the live throttle back to full rate exactly when the network was worst. Add it back here
     * once it has a real mechanism.
     */
    private static final Set<ActionType> LIVE_APPLICABLE = EnumSet.of(
            ActionType.REDUCE_LOW_PRIORITY_RATE, ActionType.AGGREGATE_UPDATES, ActionType.ENABLE_DEAD_RECKONING);

    public List<CandidateAction> generate(List<ConsistencyRiskAssessment> assessments) {
        List<CandidateAction> candidates = new ArrayList<>();
        candidates.add(CandidateAction.noAction());

        List<ConsistencyRiskAssessment> atRisk = assessments.stream()
                .filter(a -> a.risk == RiskLevel.HIGH)
                .toList();
        if (atRisk.isEmpty()) {
            return candidates; // nothing at risk — NO_ACTION is the only sensible candidate
        }

        List<String> lowPriorityTargets = atRisk.stream()
                .filter(a -> a.element.getPriority() == StatePriority.LOW || a.element.getPriority() == StatePriority.VERY_LOW
                        || a.element.getPriority() == StatePriority.MODERATE)
                .map(a -> a.element.getElementName())
                .collect(Collectors.toList());
        if (!lowPriorityTargets.isEmpty()) {
            candidates.add(new CandidateAction(ActionType.REDUCE_LOW_PRIORITY_RATE, lowPriorityTargets,
                    Map.of("frequencyMultiplier", 0.5)));
        }

        List<String> predictableTargets = atRisk.stream()
                .filter(a -> a.element.isPredictionSupported())
                .map(a -> a.element.getElementName())
                .collect(Collectors.toList());
        if (!predictableTargets.isEmpty()) {
            candidates.add(new CandidateAction(ActionType.ENABLE_DEAD_RECKONING, predictableTargets, Map.of()));
        }

        List<String> aggregatableTargets = atRisk.stream()
                .filter(a -> a.element.isAggregationSupported())
                .map(a -> a.element.getElementName())
                .collect(Collectors.toList());
        if (aggregatableTargets.size() > 1) {
            candidates.add(new CandidateAction(ActionType.AGGREGATE_UPDATES, aggregatableTargets,
                    Map.of("windowMs", 50)));
        }

        List<String> criticalTargets = atRisk.stream()
                .filter(a -> a.element.getPriority() == StatePriority.CRITICAL || a.element.getPriority() == StatePriority.HIGH)
                .map(a -> a.element.getElementName())
                .collect(Collectors.toList());
        if (!criticalTargets.isEmpty()) {
            candidates.add(new CandidateAction(ActionType.PRIORITIZE_CRITICAL, criticalTargets, Map.of()));
        }

        candidates.removeIf(c -> c.actionType != ActionType.NO_ACTION && !LIVE_APPLICABLE.contains(c.actionType));
        return candidates;
    }
}
