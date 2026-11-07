package com.nexora.network.telemetry;

/** One point of the session-level telemetry time series (docs/DATABASE_DESIGN.md's
 *  network_telemetry, aggregated across players in the session — see TelemetryCollector). */
public class NetworkSample {
    public final long recordedAtMillis;
    public final double rttMs;
    public final double jitterMs;
    public final double packetLossPct;
    public final double throughputPacketsPerSec;

    public NetworkSample(long recordedAtMillis, double rttMs, double jitterMs,
                          double packetLossPct, double throughputPacketsPerSec) {
        this.recordedAtMillis = recordedAtMillis;
        this.rttMs = rttMs;
        this.jitterMs = jitterMs;
        this.packetLossPct = packetLossPct;
        this.throughputPacketsPerSec = throughputPacketsPerSec;
    }
}
