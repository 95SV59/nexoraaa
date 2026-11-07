package com.nexora.multiplayer.engine;

import java.util.Map;

/** Server-computed match outcome, handed to SessionService for persistence (game_session's
 *  match_result.result_summary is exactly this, serialized to JSON). Never derived from
 *  client-reported values — see docs/ARCHITECTURE.md §8. */
public class MatchResultData {
    private final String winnerPlayerId; // nullable: draw / no winner
    private final int durationSec;
    private final Map<String, Object> summary; // per-player scores/stats, engine-defined shape

    public MatchResultData(String winnerPlayerId, int durationSec, Map<String, Object> summary) {
        this.winnerPlayerId = winnerPlayerId;
        this.durationSec = durationSec;
        this.summary = summary;
    }

    public String getWinnerPlayerId() { return winnerPlayerId; }
    public int getDurationSec() { return durationSec; }
    public Map<String, Object> getSummary() { return summary; }
}
