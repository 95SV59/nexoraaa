package com.nexora.network.prediction;

/** Maps to network_prediction (docs/DATABASE_DESIGN.md). confidence in [0,1]. */
public class NetworkPredictionResult {
    public final long predictedAtMillis;
    public final double predictedRttMs;
    public final double predictedJitterMs;
    public final double predictedLossPct;
    public final double predictedThroughput;
    public final double confidence;
    public final long horizonMs;
    public final int basedOnSampleCount;

    public NetworkPredictionResult(long predictedAtMillis, double predictedRttMs, double predictedJitterMs,
                                    double predictedLossPct, double predictedThroughput, double confidence,
                                    long horizonMs, int basedOnSampleCount) {
        this.predictedAtMillis = predictedAtMillis;
        this.predictedRttMs = predictedRttMs;
        this.predictedJitterMs = predictedJitterMs;
        this.predictedLossPct = predictedLossPct;
        this.predictedThroughput = predictedThroughput;
        this.confidence = confidence;
        this.horizonMs = horizonMs;
        this.basedOnSampleCount = basedOnSampleCount;
    }
}
