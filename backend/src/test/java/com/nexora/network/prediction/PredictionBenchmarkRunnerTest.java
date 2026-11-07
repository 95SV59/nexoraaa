package com.nexora.network.prediction;

import com.nexora.network.telemetry.NetworkSample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PWNC Stage 201: Multi-Model Prediction Benchmark Tests")
class PredictionBenchmarkRunnerTest {

    private NetworkPredictor predictor;
    private PredictionBenchmarkRunner runner;

    @BeforeEach
    void setUp() {
        predictor = new NetworkPredictor();
        runner = new PredictionBenchmarkRunner(predictor);
    }

    private List<NetworkSample> generateSyntheticSamples(int count, double baseRtt, double slope, double jitterBase) {
        long now = System.currentTimeMillis();
        List<NetworkSample> samples = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            long timestamp = now - (count - 1 - i) * 200L; // 200ms intervals
            double rtt = baseRtt + slope * i;
            double jitter = jitterBase + (i % 3);
            samples.add(new NetworkSample(timestamp, rtt, jitter, 0.0, 60.0));
        }
        return samples;
    }

    @Test
    @DisplayName("Handles insufficient samples gracefully by returning safe baseline")
    void testInsufficientSamplesReturnsBaseline() {
        List<NetworkSample> fewSamples = generateSyntheticSamples(4, 50.0, 1.0, 3.0);
        PredictionBenchmarkRunner.BenchmarkComparisonReport report = runner.evaluateModels(fewSamples, 300);

        assertNotNull(report);
        assertEquals(0, report.totalSamplesEvaluated());
        assertEquals(PredictionModelType.LINEAR_TREND, report.optimalModel());
        assertTrue(report.scores().isEmpty());
    }

    @Test
    @DisplayName("Evaluates LINEAR_TREND, EWMA, and PERSISTENCE with valid scores and metrics")
    void testEvaluateAllModelsComputesMetrics() {
        // 20 samples with steady drift: RTT rises from 50 to 88 ms
        List<NetworkSample> samples = generateSyntheticSamples(20, 50.0, 2.0, 4.0);
        PredictionBenchmarkRunner.BenchmarkComparisonReport report = runner.evaluateModels(samples, 400);

        assertNotNull(report);
        assertEquals(20, report.totalSamplesEvaluated());
        assertNotNull(report.optimalModel());

        // Check that all 3 models were benchmarked
        assertTrue(report.scores().containsKey(PredictionModelType.LINEAR_TREND), "Must score LINEAR_TREND");
        assertTrue(report.scores().containsKey(PredictionModelType.EWMA), "Must score EWMA");
        assertTrue(report.scores().containsKey(PredictionModelType.PERSISTENCE), "Must score PERSISTENCE");

        for (PredictionModelType type : PredictionModelType.values()) {
            PredictionBenchmarkRunner.ModelBenchmarkScore score = report.scores().get(type);
            assertNotNull(score);
            assertEquals(type, score.modelType());
            assertTrue(score.evaluationsCount() > 0, "Evaluations count must be positive");
            assertTrue(score.maeRttMs() >= 0.0, "MAE RTT must be non-negative");
            assertTrue(score.rmseRttMs() >= 0.0, "RMSE RTT must be non-negative");
            assertTrue(score.maeJitterMs() >= 0.0, "MAE Jitter must be non-negative");
        }
    }

    @Test
    @DisplayName("Selects model with lowest RMSE as the optimal model")
    void testSelectsLowestRmseModel() {
        List<NetworkSample> samples = generateSyntheticSamples(25, 45.0, 1.5, 2.0);
        PredictionBenchmarkRunner.BenchmarkComparisonReport report = runner.evaluateModels(samples, 500);

        PredictionModelType bestType = report.optimalModel();
        double bestRmse = report.scores().get(bestType).rmseRttMs();

        for (PredictionBenchmarkRunner.ModelBenchmarkScore score : report.scores().values()) {
            assertTrue(bestRmse <= score.rmseRttMs(), "Optimal model must have the minimum RMSE score");
        }
    }
}
