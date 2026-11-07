package com.nexora.network.lab;

import java.util.Map;

/** Named presets matching network-lab/README.md and the network_profile values used
 *  by the (future, Phase 20) network_experiment table. */
public final class NetworkProfilePresets {
    private NetworkProfilePresets() { }

    public static final Map<String, NetworkLabProfile> PRESETS = Map.of(
            "EXCELLENT", new NetworkLabProfile("EXCELLENT", 0, 0, 0, 10_000),
            "GOOD", new NetworkLabProfile("GOOD", 30, 5, 0, 2_000),
            "MODERATE", new NetworkLabProfile("MODERATE", 80, 20, 1, 1_000),
            "POOR", new NetworkLabProfile("POOR", 180, 50, 5, 512),
            "UNSTABLE", new NetworkLabProfile("UNSTABLE", 300, 120, 12, 256)
    );

    public static NetworkLabProfile byName(String name) {
        NetworkLabProfile p = PRESETS.get(name.toUpperCase());
        if (p == null) {
            throw new IllegalArgumentException("Unknown network profile: " + name
                    + " (expected one of " + PRESETS.keySet() + ")");
        }
        return p;
    }
}
