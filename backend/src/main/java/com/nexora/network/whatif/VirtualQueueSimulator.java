package com.nexora.network.whatif;

import com.nexora.multiplayer.engine.StateElementMetadata;
import com.nexora.network.candidate.ActionType;
import com.nexora.network.candidate.CandidateAction;
import com.nexora.network.prediction.NetworkPredictionResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Patent Stage 204 (Virtual Evaluation Sandbox) & Claim 3:
 * A discrete-event message queue simulator that models outbound packet buffer dynamics,
 * transmission drain rates, and state element delivery timings over a simulated forecast horizon.
 *
 * It operates strictly on decoupled copies and state metadata without touching or mutating
 * the live GameSession or sending experimental frames over live sockets.
 */
public class VirtualQueueSimulator {

    public record QueueSimulationResult(
            double peakQueueDepth,
            double averageQueuingDelayMs,
            double totalBandwidthBytes,
            double totalMessagesDispatched,
            double bandwidthSavingsPct,
            Map<String, Double> simulatedElementStalenessMs
    ) {}

    private static final int SIMULATION_TICK_INTERVAL_MS = 33; // 30Hz game loop (33.3ms)
    private static final int BASE_MESSAGE_SIZE_BYTES = 128; // Average serialized StateElement payload

    /**
     * Discrete-event queue simulation modeling outbound buffer dynamics.
     *
     * Dimensional Consistency & Physical Units:
     * - Queue Backlog Q(t): bytes [B]
     * - Outbound Link Service Rate mu: bytes per second [B/s]
     * - Drain Capacity per Tick: mu * (dt / 1000) [B/tick]
     * - Queuing Delay: (Q / mu) * 1000 [ms]
     * - One-way delay: (RTT_pred / 2) + Jitter_pred + QueuingDelay [ms]
     * - Total Element Staleness: TimeSinceLastDispatch + OneWayDelay [ms]
     */
    public static QueueSimulationResult simulate(
            CandidateAction candidate,
            List<StateElementMetadata> elements,
            NetworkPredictionResult prediction) {

        long horizonMs = Math.max(100, Math.min(2000, prediction.horizonMs));
        int totalSimTicks = (int) (horizonMs / SIMULATION_TICK_INTERVAL_MS);

        double simulatedQueueBytes = 0;
        double peakQueueBytes = 0;
        double cumulativeQueuingDelayMs = 0;
        double totalBytesGenerated = 0;
        double baselineBytesGenerated = 0;
        int messagesDispatched = 0;

        // Service capacity: derive outbound bandwidth (bytes/sec)
        // If loss > 0, TCP-friendly congestion capacity reduces drain rate
        double effectivePacketRate = Math.max(10.0, prediction.predictedThroughput > 0 ? prediction.predictedThroughput : 30.0);
        if (prediction.predictedLossPct > 0) {
            double lossFactor = Math.max(0.2, 1.0 - (prediction.predictedLossPct / 100.0));
            effectivePacketRate *= lossFactor;
        }
        double outboundServiceRateBytesPerSec = effectivePacketRate * BASE_MESSAGE_SIZE_BYTES;
        double drainCapacityBytesPerTick = Math.max(64.0, outboundServiceRateBytesPerSec * (SIMULATION_TICK_INTERVAL_MS / 1000.0));

        Map<String, Double> lastDispatchedTick = new HashMap<>();
        for (StateElementMetadata el : elements) {
            lastDispatchedTick.put(el.getElementName(), 0.0);
        }

        for (int tick = 1; tick <= totalSimTicks; tick++) {
            double tickGeneratedBytes = 0;

            for (StateElementMetadata el : elements) {
                baselineBytesGenerated += BASE_MESSAGE_SIZE_BYTES;
                boolean shouldQueueThisTick = true;

                // Candidate Action Policy Emulation
                if (candidate.actionType == ActionType.REDUCE_LOW_PRIORITY_RATE) {
                    if (candidate.targetElementNames.contains(el.getElementName())) {
                        // Throttled to half rate: only queue on even ticks
                        shouldQueueThisTick = (tick % 2 == 0);
                    }
                } else if (candidate.actionType == ActionType.ENABLE_DEAD_RECKONING) {
                    if (candidate.targetElementNames.contains(el.getElementName())) {
                        // Dead reckoning transmits keyframes only every 3rd tick
                        shouldQueueThisTick = (tick % 3 == 0);
                    }
                } else if (candidate.actionType == ActionType.AGGREGATE_UPDATES) {
                    if (candidate.targetElementNames.contains(el.getElementName())) {
                        // Aggregated updates combine headers, saving 40% byte overhead
                        tickGeneratedBytes += (BASE_MESSAGE_SIZE_BYTES * 0.6);
                        shouldQueueThisTick = false;
                        lastDispatchedTick.put(el.getElementName(), (double) tick);
                    }
                }

                if (shouldQueueThisTick) {
                    tickGeneratedBytes += BASE_MESSAGE_SIZE_BYTES;
                    lastDispatchedTick.put(el.getElementName(), (double) tick);
                }
            }

            totalBytesGenerated += tickGeneratedBytes;
            simulatedQueueBytes += tickGeneratedBytes;

            if (simulatedQueueBytes > peakQueueBytes) {
                peakQueueBytes = simulatedQueueBytes;
            }

            // Simulate outbound queue drain
            double bytesDrained = Math.min(simulatedQueueBytes, drainCapacityBytesPerTick);
            simulatedQueueBytes -= bytesDrained;
            if (bytesDrained > 0) {
                messagesDispatched += (int) Math.ceil(bytesDrained / BASE_MESSAGE_SIZE_BYTES);
            }

            // Estimate current queuing delay: (buffered bytes / drain rate) * tickMs
            double currentQueuingDelayMs = (simulatedQueueBytes / Math.max(1, drainCapacityBytesPerTick)) * SIMULATION_TICK_INTERVAL_MS;
            cumulativeQueuingDelayMs += currentQueuingDelayMs;
        }

        double avgQueuingDelayMs = cumulativeQueuingDelayMs / Math.max(1, totalSimTicks);
        double savingsPct = baselineBytesGenerated > 0
                ? Math.max(0, (baselineBytesGenerated - totalBytesGenerated) / baselineBytesGenerated * 100.0)
                : 0.0;

        // Calculate simulated staleness by element
        Map<String, Double> stalenessMap = new HashMap<>();
        double oneWayDelayMs = (prediction.predictedRttMs / 2.0) + prediction.predictedJitterMs + avgQueuingDelayMs;
        for (StateElementMetadata el : elements) {
            double lastTick = lastDispatchedTick.getOrDefault(el.getElementName(), (double) totalSimTicks);
            double ticksSinceLastDispatch = totalSimTicks - lastTick;
            double timeSinceDispatchMs = ticksSinceLastDispatch * SIMULATION_TICK_INTERVAL_MS;
            stalenessMap.put(el.getElementName(), timeSinceDispatchMs + oneWayDelayMs);
        }

        return new QueueSimulationResult(
                peakQueueBytes / BASE_MESSAGE_SIZE_BYTES,
                avgQueuingDelayMs,
                totalBytesGenerated,
                messagesDispatched,
                savingsPct,
                stalenessMap
        );
    }
}
