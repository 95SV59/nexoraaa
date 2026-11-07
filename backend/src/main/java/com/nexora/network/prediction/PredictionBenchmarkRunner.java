package com.nexora.network.prediction;

import com.nexora.network.telemetry.NetworkSample;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Patent Stage 201 (Future Network Prediction) & Research Requirement §7.3:
 * Evaluates and benchmarks alternative prediction models (LINEAR_TREND vs EWMA vs PERSISTENCE)
 * against identical historical telemetry data across configured forecast horizons.
 *
 * Computes:
 *  - Mean Absolute Error (MAE) for RTT and Jitter
 *  - Root Mean Square Error (RMSE) for RTT
 *  - Prediction Bias (signed error)
 */
@Component
public class PredictionBenchmarkRunner {

    public record ModelBenchmarkScore(
            PredictionModelType modelType,
            double maeRttMs,
            double rmseRttMs,
            double maeJitterMs,
            double predictionBiasMs,
            int evaluationsCount
    ) {}

    public record BenchmarkComparisonReport(
            long horizonMs,
            int totalSamplesEvaluated,
            Map<PredictionModelType, ModelBenchmarkScore> scores,
            PredictionModelType optimalModel
    ) {}

    private final NetworkPredictor predictor;

    public PredictionBenchmarkRunner(NetworkPredictor predictor) {
        this.predictor = predictor;
    }

    /**
     * Runs rolling-origin time-series cross-validation over the supplied telemetry samples.
     */
    public BenchmarkComparisonReport evaluateModels(List<NetworkSample> samples, long horizonMs) {
        if (samples == null || samples.size() < 6) {
            return new BenchmarkComparisonReport(horizonMs, 0, Map.of(), PredictionModelType.LINEAR_TREND);
        }

        Map<PredictionModelType, List<Double>> rttErrors = new EnumMap<>(PredictionModelType.class);
        Map<PredictionModelType, List<Double>> jitterErrors = new EnumMap<>(PredictionModelType.class);

        for (PredictionModelType model : PredictionModelType.values()) {
            rttErrors.put(model, new ArrayList<>());
            jitterErrors.put(model, new ArrayList<>());
        }

        int windowSize = 5;
        // Step forward one sample at a time (rolling origin evaluation)
        for (int i = windowSize; i < samples.size() - 1; i++) {
            List<NetworkSample> window = samples.subList(i - windowSize, i);
            NetworkSample actualTarget = samples.get(i);

            for (PredictionModelType model : PredictionModelType.values()) {
                NetworkPredictionResult pred = predictor.predictWithModel(window, horizonMs, 0.0, model);
                double errRtt = actualTarget.rttMs - pred.predictedRttMs;
                double errJitter = actualTarget.jitterMs - pred.predictedJitterMs;

                rttErrors.get(model).add(errRtt);
                jitterErrors.get(model).add(errJitter);
            }
        }

        Map<PredictionModelType, ModelBenchmarkScore> scores = new EnumMap<>(PredictionModelType.class);
        PredictionModelType bestModel = PredictionModelType.LINEAR_TREND;
        double bestRmse = Double.MAX_VALUE;

        for (PredictionModelType model : PredictionModelType.values()) {
            List<Double> rErrors = rttErrors.get(model);
            List<Double> jErrors = jitterErrors.get(model);

            if (rErrors.isEmpty()) continue;

            double sumAbsRtt = 0;
            double sumSqRtt = 0;
            double sumRtt = 0;
            double sumAbsJitter = 0;
            int n = rErrors.size();

            for (int k = 0; k < n; k++) {
                double rErr = rErrors.get(k);
                double jErr = jErrors.get(k);
                sumAbsRtt += Math.abs(rErr);
                sumSqRtt += (rErr * rErr);
                sumRtt += rErr;
                sumAbsJitter += Math.abs(jErr);
            }

            double maeRtt = sumAbsRtt / n;
            double rmseRtt = Math.sqrt(sumSqRtt / n);
            double maeJitter = sumAbsJitter / n;
            double bias = sumRtt / n;

            ModelBenchmarkScore score = new ModelBenchmarkScore(model, maeRtt, rmseRtt, maeJitter, bias, n);
            scores.put(model, score);

            if (rmseRtt < bestRmse) {
                bestRmse = rmseRtt;
                bestModel = model;
            }
        }

        return new BenchmarkComparisonReport(horizonMs, samples.size(), scores, bestModel);
    }
}
