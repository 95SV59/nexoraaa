package com.nexora.multiplayer.matchmaking.dto;

public record MatchFoundMessage(
        String event, // "MATCH_FOUND"
        String roomCode,
        String gameSlug
) {}
