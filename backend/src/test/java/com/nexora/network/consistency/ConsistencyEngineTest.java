package com.nexora.network.consistency;

import com.nexora.multiplayer.engine.StateElementMetadata;
import com.nexora.multiplayer.engine.arena.NexoraArenaGame;
import com.nexora.network.prediction.NetworkPredictionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConsistencyEngineTest {

    private final ConsistencyEngine engine = new ConsistencyEngine();
    private final List<StateElementMetadata> arena = new NexoraArenaGame().getStateMetadata();

    private static NetworkPredictionResult prediction(double rtt, double jitter, double loss) {
        return new NetworkPredictionResult(System.currentTimeMillis(), rtt, jitter, loss, 60, 0.95, 3000, 15);
    }

    private RiskLevel riskOf(List<ConsistencyRiskAssessment> all, String element) {
        return all.stream().filter(a -> a.element.getElementName().equals(element))
                .findFirst().orElseThrow().risk;
    }

    @Test
    void healthyNetworkFlagsNothing() {
        List<ConsistencyRiskAssessment> r = engine.assess(prediction(5, 2, 0), arena);
        assertTrue(r.stream().allMatch(a -> a.risk == RiskLevel.LOW),
                "a perfect network must not put any Arena element at risk");
    }

    /** Regression: divergence used to be estimated with one generic speed, so aim (tolerance in
     *  radians) was flagged at ~1 ms of jitter and the controller acted on a perfect network. */
    @Test
    void aimAngleDivergenceIsEstimatedInItsOwnUnits() {
        List<ConsistencyRiskAssessment> r = engine.assess(prediction(40, 20, 0), arena);
        assertEquals(RiskLevel.LOW, riskOf(r, "player_aim"));
        assertEquals(RiskLevel.LOW, riskOf(r, "player_position"));
    }

    @Test
    void highLatencyPutsPositionAndProjectilesAtRisk() {
        List<ConsistencyRiskAssessment> r = engine.assess(prediction(200, 20, 0), arena);
        assertEquals(RiskLevel.HIGH, riskOf(r, "player_position"));
        assertEquals(RiskLevel.HIGH, riskOf(r, "projectile_position"));
    }

    @Test
    void eventDrivenStateIsOnlyAtRiskUnderLoss() {
        assertEquals(RiskLevel.LOW, riskOf(engine.assess(prediction(5, 2, 0), arena), "player_health"));
        assertEquals(RiskLevel.HIGH, riskOf(engine.assess(prediction(5, 2, 5), arena), "player_health"));
        assertEquals(RiskLevel.HIGH, riskOf(engine.assess(prediction(5, 2, 5), arena), "score"));
    }
}
