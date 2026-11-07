// Mirrors com.nexora.network.experiment.dto.* (Phase 20). Numeric metrics are nullable
// because a session with no telemetry yet (or no broadcast stats, e.g. still running)
// legitimately has nothing to average.

export interface SessionListItem {
  sessionId: number;
  roomId: number;
  controllerMode: "BASELINE" | "PROPOSED";
  startedAt: string;
  endedAt: string | null;
}

export interface SessionSummary {
  sessionId: number;
  controllerMode: "BASELINE" | "PROPOSED" | null;
  telemetrySampleCount: number;
  avgRttMs: number | null;
  avgJitterMs: number | null;
  avgPacketLossPct: number | null;
  avgPredictionConfidence: number | null;
  avgAbsRttErrorMs: number | null;
  avgAbsLossErrorPct: number | null;
  totalDecisionCycles: number;
  interventionCount: number;
  interventionRatePct: number | null;
  totalTicks: number | null;
  totalBroadcasts: number | null;
  broadcastRatioPct: number | null;
  confidenceGatePassRatePct: number | null;
  avgConsistencyCost: number | null;
  avgTotalScore: number | null;
}

export interface ComparisonReport {
  baseline: SessionSummary;
  proposed: SessionSummary;
  rttImprovementMs: number | null;
  lossImprovementPct: number | null;
  /** Percentage points fewer GAME_STATE broadcasts in PROPOSED vs BASELINE. */
  broadcastReductionPct: number | null;
  consistencyCostReductionPct: number | null;
  scoreImprovementPct: number | null;
}

export interface ProfileBenchmarkResult {
  profileName: string;
  latencyMs: number;
  jitterMs: number;
  packetLossPct: number;
  baselineAvgRttMs: number;
  proposedAvgRttMs: number;
  rttImprovementMs: number;
  baselineBroadcastRatioPct: number;
  proposedBroadcastRatioPct: number;
  broadcastReductionPct: number;
  totalCycles: number;
  interventionCount: number;
  confidenceGatePassRatePct: number;
  avgConsistencyCost: number;
  avgAbsRttErrorMs: number;
}

export interface BatchEvaluationResult {
  model: string;
  timestamp: number;
  profiles: ProfileBenchmarkResult[];
}
