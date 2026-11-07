package com.nexora.network.candidate;

import com.nexora.multiplayer.engine.StateElementMetadata;
import com.nexora.multiplayer.engine.arena.NexoraArenaGame;
import com.nexora.network.consistency.ConsistencyEngine;
import com.nexora.network.consistency.ConsistencyRiskAssessment;
import com.nexora.network.prediction.NetworkPredictionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CandidateGeneratorTest {

    private final CandidateGenerator generator = new CandidateGenerator();
    private final List<StateElementMetadata> arena = new NexoraArenaGame().getStateMetadata();

    private List<ConsistencyRiskAssessment> risks(double rtt, double jitter, double loss) {
        return new ConsistencyEngine().assess(
                new NetworkPredictionResult(System.currentTimeMillis(), rtt, jitter, loss, 60, 0.95, 3000, 15), arena);
    }

    @Test
    void alwaysIncludesNoActionAsTheBaseline() {
        assertTrue(generator.generate(risks(5, 2, 0)).stream().anyMatch(c -> c.actionType == ActionType.NO_ACTION));
        assertTrue(generator.generate(risks(250, 60, 8)).stream().anyMatch(c -> c.actionType == ActionType.NO_ACTION));
    }

    @Test
    void healthyNetworkProposesNothingElse() {
        assertEquals(1, generator.generate(risks(5, 2, 0)).size());
    }

    /** Regression: an action the live system can't apply used to win the what-if scoring. */
    @Test
    void neverProposesAnActionTheSystemCannotApply() {
        List<CandidateAction> c = generator.generate(risks(250, 60, 8));
        assertTrue(c.stream().noneMatch(a -> a.actionType == ActionType.PRIORITIZE_CRITICAL));
        assertTrue(c.stream().anyMatch(a -> a.actionType == ActionType.ENABLE_DEAD_RECKONING));
    }
}
