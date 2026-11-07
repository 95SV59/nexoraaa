package com.nexora.network.experiment.dto;

public class ComparisonReportDto {
    public SessionSummaryDto baseline;
    public SessionSummaryDto proposed;

    /** Positive means PROPOSED had lower (better) average RTT than BASELINE, in ms. Null if
     *  either session has no telemetry yet. Left as raw deltas rather than a verdict —
     *  Phase 20's report should draw conclusions from real experiment runs, not this code. */
    public Double rttImprovementMs;
    public Double lossImprovementPct;

    /** The headline Phase 20 result: percentage points fewer GAME_STATE broadcasts sent
     *  in PROPOSED vs BASELINE under the identical NetworkLabProfile. Positive means the
     *  controller measurably reduced network load. */
    public Double broadcastReductionPct;

    /** Positive means PROPOSED reduced consistency violation costs compared to BASELINE. */
    public Double consistencyCostReductionPct;

    /** Positive means PROPOSED had lower overall What-If evaluation score/penalty. */
    public Double scoreImprovementPct;

    public static ComparisonReportDto of(SessionSummaryDto baseline, SessionSummaryDto proposed) {
        ComparisonReportDto report = new ComparisonReportDto();
        report.baseline = baseline;
        report.proposed = proposed;
        if (baseline.avgRttMs != null && proposed.avgRttMs != null) {
            report.rttImprovementMs = baseline.avgRttMs - proposed.avgRttMs;
        }
        if (baseline.avgPacketLossPct != null && proposed.avgPacketLossPct != null) {
            report.lossImprovementPct = baseline.avgPacketLossPct - proposed.avgPacketLossPct;
        }
        if (baseline.broadcastRatioPct != null && proposed.broadcastRatioPct != null) {
            report.broadcastReductionPct = baseline.broadcastRatioPct - proposed.broadcastRatioPct;
        }
        if (baseline.avgConsistencyCost != null && proposed.avgConsistencyCost != null && baseline.avgConsistencyCost > 0) {
            report.consistencyCostReductionPct = 100.0 * (baseline.avgConsistencyCost - proposed.avgConsistencyCost) / baseline.avgConsistencyCost;
        }
        if (baseline.avgTotalScore != null && proposed.avgTotalScore != null && baseline.avgTotalScore > 0) {
            report.scoreImprovementPct = 100.0 * (baseline.avgTotalScore - proposed.avgTotalScore) / baseline.avgTotalScore;
        }
        return report;
    }
}
