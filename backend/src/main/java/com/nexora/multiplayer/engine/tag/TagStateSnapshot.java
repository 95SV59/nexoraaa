package com.nexora.multiplayer.engine.tag;

import java.util.List;

public class TagStateSnapshot {
    public final String gameId = "tag";
    public final String roomId;
    public final long serverTimestamp;
    public final List<TagPlayerSnapshot> players;
    public final String currentItPlayerId;
    public final int timeRemainingSec;
    public final boolean matchOver;
    public final String winnerPlayerId;
    public final String lastEvent;

    public TagStateSnapshot(String roomId, long serverTimestamp, List<TagPlayerSnapshot> players,
                            String currentItPlayerId, int timeRemainingSec, boolean matchOver,
                            String winnerPlayerId, String lastEvent) {
        this.roomId = roomId;
        this.serverTimestamp = serverTimestamp;
        this.players = players;
        this.currentItPlayerId = currentItPlayerId;
        this.timeRemainingSec = timeRemainingSec;
        this.matchOver = matchOver;
        this.winnerPlayerId = winnerPlayerId;
        this.lastEvent = lastEvent;
    }

    public static class TagPlayerSnapshot {
        public final String playerId;
        public final String displayName;
        public final double x;
        public final double y;
        public final double vx;
        public final double vy;
        public final boolean isIt;
        public final double timeAsItSec;
        public final boolean immune;

        public TagPlayerSnapshot(String playerId, String displayName, double x, double y,
                                 double vx, double vy, boolean isIt, double timeAsItSec, boolean immune) {
            this.playerId = playerId;
            this.displayName = displayName;
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.isIt = isIt;
            this.timeAsItSec = timeAsItSec;
            this.immune = immune;
        }
    }
}
