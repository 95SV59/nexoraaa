package com.nexora.network.feedback;

/** Maps to prediction_error (docs/DATABASE_DESIGN.md). */
public class PredictionErrorRecord {
    public final long measuredAtMillis;
    public final double predictedRttMs;
    public final double actualRttMs;
    public final double rttError; // actual - predicted
    public final double predictedLossPct;
    public final double actualLossPct;
    public final double lossError;

    public PredictionErrorRecord(long measuredAtMillis, double predictedRttMs, double actualRttMs,
                                  double predictedLossPct, double actualLossPct) {
        this.measuredAtMillis = measuredAtMillis;
        this.predictedRttMs = predictedRttMs;
        this.actualRttMs = actualRttMs;
        this.rttError = actualRttMs - predictedRttMs;
        this.predictedLossPct = predictedLossPct;
        this.actualLossPct = actualLossPct;
        this.lossError = actualLossPct - predictedLossPct;
    }
}
