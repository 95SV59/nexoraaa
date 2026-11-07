package com.nexora.network.experiment.dto;

import java.util.ArrayList;
import java.util.List;

public class BatchEvaluationResultDto {
    public String model;
    public long timestamp;
    public List<ProfileResult> profiles = new ArrayList<>();

    public static class ProfileResult {
        public String profileName;
        public double latencyMs;
        public double jitterMs;
        public double packetLossPct;
        public double baselineAvgRttMs;
        public double proposedAvgRttMs;
        public double rttImprovementMs;
        public double baselineBroadcastRatioPct;
        public double proposedBroadcastRatioPct;
        public double broadcastReductionPct;
        public int totalCycles;
        public int interventionCount;
        public double confidenceGatePassRatePct;
        public double avgConsistencyCost;
        public double avgAbsRttErrorMs;
    }
}
