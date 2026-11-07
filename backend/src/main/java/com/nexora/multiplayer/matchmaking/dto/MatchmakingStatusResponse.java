package com.nexora.multiplayer.matchmaking.dto;

public record MatchmakingStatusResponse(
        String status, // "IDLE", "QUEUED", "MATCHED"
        String gameSlug,
        String roomCode,
        long elapsedSeconds,
        int queueSize
) {}
