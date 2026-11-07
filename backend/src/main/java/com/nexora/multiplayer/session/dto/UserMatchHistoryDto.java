package com.nexora.multiplayer.session.dto;

import java.time.Instant;

public record UserMatchHistoryDto(
        Long sessionId,
        String roomCode,
        String gameSlug,
        Instant startedAt,
        Instant endedAt,
        Integer durationSec,
        boolean won,
        Long winnerUserId,
        String controllerMode,
        String resultSummaryJson
) {}
