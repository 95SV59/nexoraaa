package com.nexora.network.lab;

/**
 * One artificial-degradation profile for a room. bandwidthKbps is accepted for
 * interface/schema completeness (matches network_experiment.bandwidth_kbps in
 * docs/DATABASE_DESIGN.md) but is NOT yet enforced — see network-lab/README.md's
 * "Implementation Note" for why literal bandwidth throttling at the message layer
 * was left out of this MVP.
 */
public record NetworkLabProfile(String profileName, double latencyMs, double jitterMs,
                                 double packetLossPct, double bandwidthKbps) {

    public static NetworkLabProfile off() {
        return new NetworkLabProfile("OFF", 0, 0, 0, 0);
    }

    public boolean isOff() {
        return latencyMs <= 0 && jitterMs <= 0 && packetLossPct <= 0;
    }
}
