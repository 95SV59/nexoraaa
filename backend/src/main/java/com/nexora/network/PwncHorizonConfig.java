package com.nexora.network;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * P0.5 Centralized Horizon Configuration:
 * Aligns prediction horizon, virtual sandbox evaluation window, and feedback outcome measurement.
 * Prevents premature feedback evaluation and ensures scientific consistency across experiments.
 */
@Component
public class PwncHorizonConfig {

    private final int predictionHorizonMs;
    private final int simulationHorizonMs;
    private final int feedbackEvaluationDelayMs;
    private final int cycleIntervalMs;

    public PwncHorizonConfig(
            @Value("${nexora.network.prediction-horizon-ms:500}") int predictionHorizonMs,
            @Value("${nexora.network.simulation-horizon-ms:500}") int simulationHorizonMs,
            @Value("${nexora.network.cycle-interval-ms:200}") int cycleIntervalMs) {
        this.predictionHorizonMs = Math.max(100, Math.min(2000, predictionHorizonMs));
        this.simulationHorizonMs = Math.max(100, Math.min(2000, simulationHorizonMs));
        this.feedbackEvaluationDelayMs = this.predictionHorizonMs;
        this.cycleIntervalMs = Math.max(50, cycleIntervalMs);
    }

    public PwncHorizonConfig() {
        this(500, 500, 200);
    }

    public int getPredictionHorizonMs() { return predictionHorizonMs; }
    public int getSimulationHorizonMs() { return simulationHorizonMs; }
    public int getFeedbackEvaluationDelayMs() { return feedbackEvaluationDelayMs; }
    public int getCycleIntervalMs() { return cycleIntervalMs; }
}
