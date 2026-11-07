package com.nexora.multiplayer.room.dto;

public class PlayerSummary {
    public Long userId;
    public String displayName;
    public boolean ready;
    public boolean bot;
    public boolean spectator;
    public Integer pingMs;

    public PlayerSummary(Long userId, String displayName, boolean ready, boolean bot, Integer pingMs) {
        this(userId, displayName, ready, bot, false, pingMs);
    }

    public PlayerSummary(Long userId, String displayName, boolean ready, boolean bot, boolean spectator, Integer pingMs) {
        this.userId = userId;
        this.displayName = displayName;
        this.ready = ready;
        this.bot = bot;
        this.spectator = spectator;
        this.pingMs = pingMs;
    }
}
