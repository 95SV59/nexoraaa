package com.nexora.network.experiment.dto;

import jakarta.validation.constraints.NotBlank;

/** POST /api/experiments body — a labeling record for a baseline-vs-proposed run pair. */
public class CreateExperimentRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String gameId;
    private String networkProfile;
    private Integer latencyMs;
    private Double packetLossPct;
    private Integer jitterMs;
    private Integer bandwidthKbps;
    private Integer numPlayers;
    private Integer durationSec;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public String getNetworkProfile() { return networkProfile; }
    public void setNetworkProfile(String networkProfile) { this.networkProfile = networkProfile; }
    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
    public Double getPacketLossPct() { return packetLossPct; }
    public void setPacketLossPct(Double packetLossPct) { this.packetLossPct = packetLossPct; }
    public Integer getJitterMs() { return jitterMs; }
    public void setJitterMs(Integer jitterMs) { this.jitterMs = jitterMs; }
    public Integer getBandwidthKbps() { return bandwidthKbps; }
    public void setBandwidthKbps(Integer bandwidthKbps) { this.bandwidthKbps = bandwidthKbps; }
    public Integer getNumPlayers() { return numPlayers; }
    public void setNumPlayers(Integer numPlayers) { this.numPlayers = numPlayers; }
    public Integer getDurationSec() { return durationSec; }
    public void setDurationSec(Integer durationSec) { this.durationSec = durationSec; }
}
