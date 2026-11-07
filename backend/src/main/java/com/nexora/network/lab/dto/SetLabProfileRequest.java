package com.nexora.network.lab.dto;

/** POST /api/network-lab/{roomCode} body. Either set profileName to a preset
 *  ("EXCELLENT"/"GOOD"/"MODERATE"/"POOR"/"UNSTABLE") or leave it null and supply
 *  explicit values for a "CUSTOM" profile. */
public class SetLabProfileRequest {
    private String profileName; // null => custom
    private Double latencyMs;
    private Double jitterMs;
    private Double packetLossPct;
    private Double bandwidthKbps;

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }
    public Double getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Double latencyMs) { this.latencyMs = latencyMs; }
    public Double getJitterMs() { return jitterMs; }
    public void setJitterMs(Double jitterMs) { this.jitterMs = jitterMs; }
    public Double getPacketLossPct() { return packetLossPct; }
    public void setPacketLossPct(Double packetLossPct) { this.packetLossPct = packetLossPct; }
    public Double getBandwidthKbps() { return bandwidthKbps; }
    public void setBandwidthKbps(Double bandwidthKbps) { this.bandwidthKbps = bandwidthKbps; }
}
