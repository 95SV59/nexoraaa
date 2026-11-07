package com.nexora.multiplayer.websocket.dto;

import com.nexora.network.consistency.ConsistencyRiskAssessment;
import com.nexora.network.decision.ControllerDecisionResult;
import com.nexora.network.prediction.NetworkPredictionResult;
import com.nexora.network.whatif.WhatIfResult;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Broadcast to /topic/room/{roomCode}/network every orchestration cycle. This is the
 * single message a Research Dashboard (Phase 17) needs: future-condition prediction,
 * per-element risk, every candidate's virtual evaluation, and the final decision —
 * exactly steps 11-16 of the Final Demonstration in the master prompt.
 */
public class NetworkControllerSnapshotMessage {
    public String roomCode;
    public long timestamp;
    public PredictionView prediction;
    public List<RiskView> risks;
    public List<CandidateView> candidates;
    public DecisionView decision;

    public static NetworkControllerSnapshotMessage build(String roomCode, NetworkPredictionResult prediction,
                                                            List<ConsistencyRiskAssessment> risks,
                                                            ControllerDecisionResult decision,
                                                            int appliedBroadcastEveryNTicks) {
        NetworkControllerSnapshotMessage msg = new NetworkControllerSnapshotMessage();
        msg.roomCode = roomCode;
        msg.timestamp = System.currentTimeMillis();

        msg.prediction = new PredictionView(prediction.predictedRttMs, prediction.predictedJitterMs,
                prediction.predictedLossPct, prediction.predictedThroughput, prediction.confidence,
                prediction.horizonMs, prediction.basedOnSampleCount);

        msg.risks = risks.stream().map(r -> new RiskView(
                r.element.getElementName(), r.element.getPriority().name(), r.risk.name(), r.reason,
                r.predictedStalenessMs, r.predictedDivergenceEstimate)).collect(Collectors.toList());

        msg.candidates = decision.allEvaluations.stream().map(w -> new CandidateView(
                w.candidate.actionType.name(), w.candidate.targetElementNames,
                w.consistencyCost, w.networkCost, w.processingCost, w.controlOverhead, w.totalScore))
                .collect(Collectors.toList());

        msg.decision = new DecisionView(decision.selected.actionType.name(), decision.selected.targetElementNames,
                decision.confidenceGatePassed, decision.reason, appliedBroadcastEveryNTicks);

        return msg;
    }

    public static class PredictionView {
        public double rttMs, jitterMs, lossPct, throughput, confidence;
        public long horizonMs;
        public int sampleCount;
        public PredictionView(double rttMs, double jitterMs, double lossPct, double throughput,
                               double confidence, long horizonMs, int sampleCount) {
            this.rttMs = rttMs; this.jitterMs = jitterMs; this.lossPct = lossPct;
            this.throughput = throughput; this.confidence = confidence;
            this.horizonMs = horizonMs; this.sampleCount = sampleCount;
        }
    }

    public static class RiskView {
        public String elementName, priority, risk, reason;
        public double predictedStalenessMs, predictedDivergenceEstimate;
        public RiskView(String elementName, String priority, String risk, String reason,
                         double predictedStalenessMs, double predictedDivergenceEstimate) {
            this.elementName = elementName; this.priority = priority; this.risk = risk; this.reason = reason;
            this.predictedStalenessMs = predictedStalenessMs; this.predictedDivergenceEstimate = predictedDivergenceEstimate;
        }
    }

    public static class CandidateView {
        public String actionType;
        public List<String> targetElementNames;
        public double consistencyCost, networkCost, processingCost, controlOverhead, totalScore;
        public CandidateView(String actionType, List<String> targetElementNames, double consistencyCost,
                              double networkCost, double processingCost, double controlOverhead, double totalScore) {
            this.actionType = actionType; this.targetElementNames = targetElementNames;
            this.consistencyCost = consistencyCost; this.networkCost = networkCost;
            this.processingCost = processingCost; this.controlOverhead = controlOverhead; this.totalScore = totalScore;
        }
    }

    public static class DecisionView {
        public String actionType;
        public List<String> targetElementNames;
        public boolean confidenceGatePassed;
        public String reason;
        public int appliedBroadcastEveryNTicks;
        public DecisionView(String actionType, List<String> targetElementNames, boolean confidenceGatePassed,
                             String reason, int appliedBroadcastEveryNTicks) {
            this.actionType = actionType; this.targetElementNames = targetElementNames;
            this.confidenceGatePassed = confidenceGatePassed; this.reason = reason;
            this.appliedBroadcastEveryNTicks = appliedBroadcastEveryNTicks;
        }
    }
}
