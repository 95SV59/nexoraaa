package com.nexora.network.experiment.dto;

public class SessionSummaryDto {
    public Long sessionId;
    public String controllerMode; // BASELINE or PROPOSED
    public int telemetrySampleCount;
    public Double avgRttMs;
    public Double avgJitterMs;
    public Double avgPacketLossPct;
    public Double avgPredictionConfidence;
    public Double avgAbsRttErrorMs;   // prediction accuracy: |actual - predicted| RTT
    public Double avgAbsLossErrorPct; // prediction accuracy: |actual - predicted| loss
    public int totalDecisionCycles;
    public int interventionCount;     // cycles where a non-NO_ACTION candidate was actually selected
    public Double interventionRatePct;
    public Long totalTicks;
    public Long totalBroadcasts;
    public Double broadcastRatioPct;  // totalBroadcasts / totalTicks * 100 — ~100% for BASELINE always
    public Double confidenceGatePassRatePct; // % of decision cycles where confidence gate passed
    public Double avgConsistencyCost;        // average consistency cost of selected candidates
    public Double avgTotalScore;             // average composite cost score of selected candidates
}
