package com.nexora.network.consistency;

import com.nexora.multiplayer.engine.StateElementMetadata;
import com.nexora.network.prediction.NetworkPredictionResult;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 12 — Game-State Consistency Engine. Maps a predicted network condition onto a
 * per-element risk of violating that element's own maxStaleness/maxDivergence, using
 * only the generic StateElementMetadata every MultiplayerGame declares (docs/
 * ARCHITECTURE.md §3, §6) — this class has no knowledge of Arena, Racers, or any other
 * specific game, which is the whole point: it works unchanged for Phase 9's games.
 *
 * One simplifying assumption, called out rather than hidden: "packet loss" here models
 * message non-delivery risk under degraded conditions in the abstract, not literal UDP-style
 * drops (WebSocket/STOMP runs over TCP).
 *
 * Divergence is estimated per element as predicted jitter x that element's own declared
 * maxRateOfChangePerSec, so it is in the same units as the element's maxDivergence. (An earlier
 * version used one generic entity speed for everything, which flagged aim angles — tolerance in
 * radians — as violated at ~1 ms of jitter; a simulation of the pipeline against a perfect
 * network exposed it.)
 */
@Component
public class ConsistencyEngine {

    private static final double EVENT_DRIVEN_LOSS_RISK_THRESHOLD_PCT = 2.0;

    public List<ConsistencyRiskAssessment> assess(NetworkPredictionResult prediction,
                                                    List<StateElementMetadata> elements) {
        List<ConsistencyRiskAssessment> results = new ArrayList<>();
        double predictedOneWayDelayMs = prediction.predictedRttMs / 2.0 + prediction.predictedJitterMs;

        for (StateElementMetadata element : elements) {
            results.add(assessOne(element, prediction, predictedOneWayDelayMs));
        }
        return results;
    }

    private ConsistencyRiskAssessment assessOne(StateElementMetadata element, NetworkPredictionResult prediction,
                                                  double predictedOneWayDelayMs) {
        boolean eventDriven = element.getUpdateFrequencyHz() <= 0;

        double updateIntervalMs = eventDriven ? 0 : 1000.0 / element.getUpdateFrequencyHz();
        double predictedStalenessMs = updateIntervalMs + predictedOneWayDelayMs;
        double predictedDivergence = (prediction.predictedJitterMs / 1000.0) * element.getMaxRateOfChangePerSec();

        boolean stalenessViolation = element.getMaxStalenessMs() > 0
                && predictedStalenessMs > element.getMaxStalenessMs();
        boolean divergenceViolation = element.getMaxDivergence() > 0
                && predictedDivergence > element.getMaxDivergence();
        boolean eventLossRisk = eventDriven && prediction.predictedLossPct > EVENT_DRIVEN_LOSS_RISK_THRESHOLD_PCT;

        if (stalenessViolation || divergenceViolation || eventLossRisk) {
            String reason = String.join("; ", nonEmpty(
                    stalenessViolation ? String.format("predicted staleness %.0fms > max %dms",
                            predictedStalenessMs, element.getMaxStalenessMs()) : null,
                    divergenceViolation ? String.format("predicted divergence %.1f > max %.1f",
                            predictedDivergence, element.getMaxDivergence()) : null,
                    eventLossRisk ? String.format("event-driven element at %.1f%% predicted loss",
                            prediction.predictedLossPct) : null));
            return new ConsistencyRiskAssessment(element, RiskLevel.HIGH, reason,
                    predictedStalenessMs, predictedDivergence);
        }

        return new ConsistencyRiskAssessment(element, RiskLevel.LOW, "within constraints",
                predictedStalenessMs, predictedDivergence);
    }

    private static List<String> nonEmpty(String... reasons) {
        List<String> out = new ArrayList<>();
        for (String r : reasons) if (r != null) out.add(r);
        return out;
    }
}
