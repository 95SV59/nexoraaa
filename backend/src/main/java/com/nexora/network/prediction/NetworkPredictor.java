package com.nexora.network.prediction;

import com.nexora.network.telemetry.NetworkSample;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Phase 11 — Future Network Prediction. Deliberately simple (linear-trend extrapolation
 * over the recent telemetry window) so the mechanism described in
 * docs/PATENT_TECHNICAL_DOCUMENT.md §4.2 is demonstrable end to end; the mechanism is not
 * limited to this model class — swapping in an ARIMA/Kalman-filter/ML model later only
 * requires changing this class, since everything downstream depends only on
 * NetworkPredictionResult.
 */
@Component
public class NetworkPredictor {

    private static final int MIN_SAMPLES_FOR_TREND = 3;

    /**
     * @param recentSamples oldest-first telemetry window (TelemetryCollector.getRecentSamples)
     * @param horizonMs how far ahead to predict
     * @param rttBiasCorrectionMs additive correction fed back from FeedbackService (Phase 15);
     *                            0 until at least one prior prediction has been scored
     */
    public NetworkPredictionResult predict(List<NetworkSample> recentSamples, long horizonMs,
                                            double rttBiasCorrectionMs) {
        return predictWithModel(recentSamples, horizonMs, rttBiasCorrectionMs, PredictionModelType.LINEAR_TREND);
    }

    public NetworkPredictionResult predictWithModel(List<NetworkSample> recentSamples, long horizonMs,
                                                    double rttBiasCorrectionMs, PredictionModelType modelType) {
        long now = System.currentTimeMillis();
        if (recentSamples.isEmpty()) {
            return new NetworkPredictionResult(now, 0, 0, 0, 0, 0.05, horizonMs, 0);
        }

        if (modelType == PredictionModelType.PERSISTENCE || recentSamples.size() < MIN_SAMPLES_FOR_TREND) {
            // Naive last-value baseline: RTT(t+H) = RTT(t)
            NetworkSample latest = recentSamples.get(recentSamples.size() - 1);
            return new NetworkPredictionResult(now, Math.max(0, latest.rttMs + rttBiasCorrectionMs),
                    latest.jitterMs, latest.packetLossPct, latest.throughputPacketsPerSec,
                    0.25, horizonMs, recentSamples.size());
        }

        if (modelType == PredictionModelType.EWMA) {
            // Exponentially Weighted Moving Average baseline (alpha = 0.3)
            double alpha = 0.3;
            double ewmaRtt = recentSamples.get(0).rttMs;
            double ewmaJitter = recentSamples.get(0).jitterMs;
            double ewmaLoss = recentSamples.get(0).packetLossPct;
            double ewmaThroughput = recentSamples.get(0).throughputPacketsPerSec;

            for (int i = 1; i < recentSamples.size(); i++) {
                NetworkSample s = recentSamples.get(i);
                ewmaRtt = alpha * s.rttMs + (1 - alpha) * ewmaRtt;
                ewmaJitter = alpha * s.jitterMs + (1 - alpha) * ewmaJitter;
                ewmaLoss = alpha * s.packetLossPct + (1 - alpha) * ewmaLoss;
                ewmaThroughput = alpha * s.throughputPacketsPerSec + (1 - alpha) * ewmaThroughput;
            }

            return new NetworkPredictionResult(now, Math.max(0, ewmaRtt + rttBiasCorrectionMs),
                    ewmaJitter, clamp(ewmaLoss, 0, 100), Math.max(0, ewmaThroughput),
                    0.50, horizonMs, recentSamples.size());
        }

        // Default: LINEAR_TREND (OLS Regression Slope over Horizon H)
        double[] t = new double[recentSamples.size()];
        double[] rtt = new double[recentSamples.size()];
        double[] jitter = new double[recentSamples.size()];
        double[] loss = new double[recentSamples.size()];
        double[] throughput = new double[recentSamples.size()];
        long t0 = recentSamples.get(0).recordedAtMillis;
        for (int i = 0; i < recentSamples.size(); i++) {
            NetworkSample s = recentSamples.get(i);
            t[i] = s.recordedAtMillis - t0;
            rtt[i] = s.rttMs;
            jitter[i] = s.jitterMs;
            loss[i] = s.packetLossPct;
            throughput[i] = s.throughputPacketsPerSec;
        }

        double futureT = (now - t0) + horizonMs;
        LinearFit rttFit = fit(t, rtt);
        LinearFit jitterFit = fit(t, jitter);
        LinearFit lossFit = fit(t, loss);
        LinearFit throughputFit = fit(t, throughput);

        double predictedRtt = Math.max(0, rttFit.at(futureT) + rttBiasCorrectionMs);
        double predictedJitter = Math.max(0, jitterFit.at(futureT));
        double predictedLoss = clamp(lossFit.at(futureT), 0, 100);
        double predictedThroughput = Math.max(0, throughputFit.at(futureT));

        double confidence = computeConfidence(recentSamples, rttFit, horizonMs);

        return new NetworkPredictionResult(now, predictedRtt, predictedJitter, predictedLoss,
                predictedThroughput, confidence, horizonMs, recentSamples.size());
    }

    private double computeConfidence(List<NetworkSample> samples, LinearFit rttFit, long horizonMs) {
        double sampleFactor = Math.min(1.0, samples.size() / 10.0);

        long spanMs = samples.get(samples.size() - 1).recordedAtMillis - samples.get(0).recordedAtMillis;
        double horizonFactor = spanMs <= 0 ? 0.3 : Math.min(1.0, spanMs / (double) (horizonMs * 3));

        double meanRtt = samples.stream().mapToDouble(s -> s.rttMs).average().orElse(0);
        double noiseFactor = meanRtt <= 1 ? 0.5 : Math.max(0.1, 1.0 - Math.min(1.0, rttFit.residualStdDev / (meanRtt + 1)));

        return clamp(sampleFactor * horizonFactor * noiseFactor, 0.05, 0.99);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Ordinary least squares fit of y = slope*x + intercept, plus residual std dev. */
    private static LinearFit fit(double[] x, double[] y) {
        int n = x.length;
        double meanX = 0, meanY = 0;
        for (int i = 0; i < n; i++) { meanX += x[i]; meanY += y[i]; }
        meanX /= n; meanY /= n;

        double num = 0, den = 0;
        for (int i = 0; i < n; i++) {
            num += (x[i] - meanX) * (y[i] - meanY);
            den += (x[i] - meanX) * (x[i] - meanX);
        }
        double slope = den == 0 ? 0 : num / den;
        double intercept = meanY - slope * meanX;

        double sqErrSum = 0;
        for (int i = 0; i < n; i++) {
            double predicted = slope * x[i] + intercept;
            sqErrSum += Math.pow(y[i] - predicted, 2);
        }
        double residualStdDev = Math.sqrt(sqErrSum / n);

        return new LinearFit(slope, intercept, residualStdDev);
    }

    private record LinearFit(double slope, double intercept, double residualStdDev) {
        double at(double x) { return slope * x + intercept; }
    }
}
