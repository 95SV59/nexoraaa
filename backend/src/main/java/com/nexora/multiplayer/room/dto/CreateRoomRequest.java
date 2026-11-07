package com.nexora.multiplayer.room.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class CreateRoomRequest {

    @NotBlank
    private String gameId;

    @Min(2) @Max(10)
    private int maxPlayers;

    private String region;

    /** "BASELINE" or "PROPOSED" (default). Set when preparing a Phase 20 experiment run —
     *  see network.experiment.ControllerModeRegistry. Ordinary play leaves this null. */
    private String controllerMode;

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getControllerMode() { return controllerMode; }
    public void setControllerMode(String controllerMode) { this.controllerMode = controllerMode; }
}
