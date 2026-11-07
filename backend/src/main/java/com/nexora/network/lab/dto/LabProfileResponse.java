package com.nexora.network.lab.dto;

import com.nexora.network.lab.NetworkLabProfile;

public class LabProfileResponse {
    public String roomCode;
    public String profileName;
    public double latencyMs, jitterMs, packetLossPct, bandwidthKbps;
    public boolean active;

    public static LabProfileResponse from(String roomCode, NetworkLabProfile profile) {
        LabProfileResponse r = new LabProfileResponse();
        r.roomCode = roomCode;
        r.profileName = profile.profileName();
        r.latencyMs = profile.latencyMs();
        r.jitterMs = profile.jitterMs();
        r.packetLossPct = profile.packetLossPct();
        r.bandwidthKbps = profile.bandwidthKbps();
        r.active = !profile.isOff();
        return r;
    }
}
