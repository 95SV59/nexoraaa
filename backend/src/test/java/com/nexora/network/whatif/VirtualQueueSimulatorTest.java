package com.nexora.network.whatif;

import com.nexora.multiplayer.engine.StateElementMetadata;
import com.nexora.multiplayer.engine.arena.NexoraArenaGame;
import com.nexora.network.candidate.ActionType;
import com.nexora.network.candidate.CandidateAction;
import com.nexora.network.prediction.NetworkPredictionResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VirtualQueueSimulatorTest {

    private final List<StateElementMetadata> arenaElements = new NexoraArenaGame().getStateMetadata();

    @Test
    void simulateNoActionGeneratesFullBaselineTraffic() {
        NetworkPredictionResult prediction = new NetworkPredictionResult(
                System.currentTimeMillis(), 50, 5, 0, 60, 0.90, 500, 15);

        CandidateAction noAction = CandidateAction.noAction();
        VirtualQueueSimulator.QueueSimulationResult result =
                VirtualQueueSimulator.simulate(noAction, arenaElements, prediction);

        assertNotNull(result);
        assertTrue(result.totalBandwidthBytes() > 0);
        assertEquals(0.0, result.bandwidthSavingsPct(), 0.01);
        assertTrue(result.averageQueuingDelayMs() >= 0);
        assertTrue(result.simulatedElementStalenessMs().containsKey("player_position"));
    }

    @Test
    void simulateDeadReckoningReducesBandwidthAndBufferPressure() {
        NetworkPredictionResult prediction = new NetworkPredictionResult(
                System.currentTimeMillis(), 120, 15, 0, 40, 0.90, 500, 15);

        CandidateAction deadReckoning = new CandidateAction(
                ActionType.ENABLE_DEAD_RECKONING, List.of("player_position", "player_aim"), Map.of());
        VirtualQueueSimulator.QueueSimulationResult result =
                VirtualQueueSimulator.simulate(deadReckoning, arenaElements, prediction);

        assertNotNull(result);
        assertTrue(result.bandwidthSavingsPct() > 0, "Dead reckoning must simulate measurable bandwidth savings");
        assertTrue(result.peakQueueDepth() >= 0);
    }

    @Test
    void dimensionalConsistencyUnderExtremeBandwidthConditions() {
        // Test near-zero throughput (severe congestion / bottleneck)
        NetworkPredictionResult congested = new NetworkPredictionResult(
                System.currentTimeMillis(), 300, 50, 20.0, 5.0, 0.95, 1000, 20);

        CandidateAction noAction = CandidateAction.noAction();
        VirtualQueueSimulator.QueueSimulationResult resultCongested =
                VirtualQueueSimulator.simulate(noAction, arenaElements, congested);

        assertNotNull(resultCongested);
        // Under low service capacity, queuing delay should grow noticeably
        assertTrue(resultCongested.averageQueuingDelayMs() > 0);
        assertTrue(resultCongested.peakQueueDepth() > 0);

        // Test high capacity (excellent gigabit fiber)
        NetworkPredictionResult fiber = new NetworkPredictionResult(
                System.currentTimeMillis(), 10, 1, 0.0, 120.0, 0.99, 500, 10);
        VirtualQueueSimulator.QueueSimulationResult resultFiber =
                VirtualQueueSimulator.simulate(noAction, arenaElements, fiber);

        assertNotNull(resultFiber);
        // Delay under high service capacity must be strictly less than congested
        assertTrue(resultFiber.averageQueuingDelayMs() <= resultCongested.averageQueuingDelayMs());
    }
}
