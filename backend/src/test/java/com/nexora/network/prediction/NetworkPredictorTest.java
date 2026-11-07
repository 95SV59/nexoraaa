package com.nexora.network.prediction;

import com.nexora.network.telemetry.NetworkSample;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NetworkPredictorTest {

    private final NetworkPredictor predictor = new NetworkPredictor();

    /** Builds a 2s-spaced window ending "now", the way TelemetryCollector's samples would look. */
    private static List<NetworkSample> window(double... rtts) {
        long now = System.currentTimeMillis();
        List<NetworkSample> out = new ArrayList<>();
        for (int i = 0; i < rtts.length; i++) {
            out.add(new NetworkSample(now - (rtts.length - 1 - i) * 2000L, rtts[i], 2, 0, 60));
        }
        return out;
    }

    private static double[] constant(int n, double v) {
        double[] a = new double[n];
        java.util.Arrays.fill(a, v);
        return a;
    }

    @Test
    void emptyWindowGivesVeryLowConfidence() {
        NetworkPredictionResult r = predictor.predict(List.of(), 3000, 0);
        assertTrue(r.confidence <= 0.1, "no telemetry must never look trustworthy, got " + r.confidence);
    }

    @Test
    void steadyRttPredictsSameValueWithHighConfidence() {
        NetworkPredictionResult r = predictor.predict(window(constant(15, 100)), 3000, 0);
        assertEquals(100.0, r.predictedRttMs, 1.0);
        assertTrue(r.confidence >= 0.9, "steady series should be confident, got " + r.confidence);
    }

    @Test
    void risingTrendPredictsAboveTheLatestSample() {
        double[] rising = new double[15];
        for (int i = 0; i < rising.length; i++) rising[i] = 50 + 10 * i; // latest = 190
        NetworkPredictionResult r = predictor.predict(window(rising), 3000, 0);
        assertTrue(r.predictedRttMs > 190, "trend should extrapolate past 190, got " + r.predictedRttMs);
    }

    @Test
    void biasCorrectionIsAppliedAdditively() {
        List<NetworkSample> w = window(constant(15, 100));
        double base = predictor.predict(w, 3000, 0).predictedRttMs;
        double corrected = predictor.predict(w, 3000, 20).predictedRttMs;
        assertEquals(base + 20, corrected, 0.5);
    }

    @Test
    void fewSamplesCapConfidence() {
        NetworkPredictionResult r = predictor.predict(window(100, 100), 3000, 0);
        assertTrue(r.confidence <= 0.25, "two samples can't justify high confidence, got " + r.confidence);
    }
}
