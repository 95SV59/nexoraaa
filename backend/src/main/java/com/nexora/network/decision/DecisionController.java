package com.nexora.network.decision;

import com.nexora.network.candidate.ActionType;
import com.nexora.network.candidate.CandidateAction;
import com.nexora.network.prediction.NetworkPredictionResult;
import com.nexora.network.whatif.WhatIfResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Phase 14 — Constraint-Aware Decision Controller & Confidence Gate.
 * In accordance with Patent Stage 206 & Claims 1, 5, 6:
 *
 * Implements a strict two-stage selection pipeline:
 *  - Stage A (Hard Invariant Checking): Immediately rejects any candidate that violates
 *    hard consistency constraints (e.g. unmitigated critical event delays or state divergence).
 *  - Stage B (Resource-Cost Optimization): Optimizes the weighted objective function J(a)
 *    exclusively among feasible candidates.
 *
 * Gated by:
 *  1. Confidence Gate (threshold >= 0.80): Safe fallback to NO_ACTION on low prediction confidence.
 *  2. Minimum Dwell-Time Hysteresis: Prevents high-frequency policy oscillation under noisy telemetry.
 */
@Component
public class DecisionController {

    private final double confidenceThreshold;
    private final long minDwellTimeMs;
    private static final String DEFAULT_ROOM = "__global__";

    public static class RoomDecisionState {
        private volatile long lastAdaptationTimeMillis = 0;
        private volatile ActionType currentActiveActionType = ActionType.NO_ACTION;

        public long getLastAdaptationTimeMillis() { return lastAdaptationTimeMillis; }
        public void setLastAdaptationTimeMillis(long t) { this.lastAdaptationTimeMillis = t; }
        public ActionType getCurrentActiveActionType() { return currentActiveActionType; }
        public void setCurrentActiveActionType(ActionType a) { this.currentActiveActionType = a; }
    }

    private final java.util.Map<String, RoomDecisionState> roomStates = new java.util.concurrent.ConcurrentHashMap<>();
    @org.springframework.beans.factory.annotation.Autowired
    public DecisionController(
            @Value("${nexora.network-controller.confidence-threshold:0.80}") double confidenceThreshold,
            @Value("${nexora.network-controller.min-dwell-time-ms:6000}") long minDwellTimeMs) {
        this.confidenceThreshold = confidenceThreshold;
        this.minDwellTimeMs = minDwellTimeMs;
    }

    public DecisionController(double confidenceThreshold) {
        this(confidenceThreshold, 0);
    }

    public void clearRoom(String roomCode) {
        if (roomCode != null) {
            roomStates.remove(roomCode);
        }
    }

    public RoomDecisionState getRoomState(String roomCode) {
        return roomStates.computeIfAbsent(roomCode != null ? roomCode : DEFAULT_ROOM, k -> new RoomDecisionState());
    }

    public ControllerDecisionResult decide(List<WhatIfResult> evaluations, NetworkPredictionResult prediction) {
        return decide(DEFAULT_ROOM, evaluations, prediction);
    }

    public ControllerDecisionResult decide(String roomCode, List<WhatIfResult> evaluations, NetworkPredictionResult prediction) {
        long now = System.currentTimeMillis();
        RoomDecisionState roomState = getRoomState(roomCode);

        if (evaluations == null || evaluations.isEmpty()) {
            throw new IllegalStateException("evaluateAll must include at least NO_ACTION");
        }

        CandidateAction noActionFallback = evaluations.stream()
                .map(r -> r.candidate)
                .filter(c -> c.actionType == ActionType.NO_ACTION)
                .findFirst()
                .orElseGet(CandidateAction::noAction);

        // =========================================================================
        // Stage A: Hard Feasibility Checking (Patent Stage 206 / Claim 5)
        // Reject candidates that violate non-negotiable invariants before optimization.
        // =========================================================================
        List<WhatIfResult> feasibleCandidates = evaluations.stream()
                .filter(r -> r.feasible)
                .toList();

        if (feasibleCandidates.isEmpty()) {
            roomState.setCurrentActiveActionType(ActionType.NO_ACTION);
            String reason = "Stage A Gate: All generated candidates violated hard consistency constraints; holding safe baseline fallback";
            return new ControllerDecisionResult(noActionFallback, false, reason, evaluations);
        }

        // =========================================================================
        // Stage B: Resource-Cost Optimization (among feasible candidates only)
        // =========================================================================
        WhatIfResult best = feasibleCandidates.stream()
                .min(Comparator.comparingDouble(r -> r.totalScore))
                .orElse(evaluations.get(0));

        if (best.candidate.actionType == ActionType.NO_ACTION) {
            roomState.setCurrentActiveActionType(ActionType.NO_ACTION);
            return new ControllerDecisionResult(best.candidate, true,
                    "NO_ACTION scored best — nothing at risk or no candidate improved on baseline", evaluations);
        }

        // =========================================================================
        // Confidence Gating (Patent Claim 6)
        // Ensure prediction confidence exceeds calibrated threshold before applying policy.
        // =========================================================================
        if (prediction.confidence < confidenceThreshold) {
            String reason = String.format(
                    "Best feasible candidate (%s) scored %.2f but prediction confidence %.2f is below threshold %.2f — holding safe baseline",
                    best.candidate.actionType, best.totalScore, prediction.confidence, confidenceThreshold);
            return new ControllerDecisionResult(noActionFallback, false, reason, evaluations);
        }

        // =========================================================================
        // Hysteresis & Minimum Dwell-Time Guard (Per-Room Scoped)
        // Prevent high-frequency policy toggling / frame oscillation
        // =========================================================================
        if (best.candidate.actionType != roomState.getCurrentActiveActionType()) {
            long elapsed = now - roomState.getLastAdaptationTimeMillis();
            if (roomState.getLastAdaptationTimeMillis() > 0 && elapsed < minDwellTimeMs) {
                CandidateAction currentPolicyCandidate = evaluations.stream()
                        .map(r -> r.candidate)
                        .filter(c -> c.actionType == roomState.getCurrentActiveActionType())
                        .findFirst()
                        .orElse(best.candidate);
                String reason = String.format(
                        "Hysteresis guard: dwelling in current policy %s for stability (%dms remaining of %dms dwell)",
                        roomState.getCurrentActiveActionType(), (minDwellTimeMs - elapsed), minDwellTimeMs);
                return new ControllerDecisionResult(currentPolicyCandidate, true, reason, evaluations);
            }
            roomState.setLastAdaptationTimeMillis(now);
            roomState.setCurrentActiveActionType(best.candidate.actionType);
        }

        String reason = String.format("Selected feasible %s (score %.2f) — confidence %.2f meets threshold %.2f",
                best.candidate.actionType, best.totalScore, prediction.confidence, confidenceThreshold);
        return new ControllerDecisionResult(best.candidate, true, reason, evaluations);
    }
}
