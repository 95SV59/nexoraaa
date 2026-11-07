package com.nexora.multiplayer.engine.football;

import java.util.List;

public class FootballStateSnapshot {
    public final String gameId = "football";
    public final String roomId;
    public final long serverTimestamp;
    public final BallSnapshot ball;
    public final List<PlayerSnapshot> players;
    public final int redScore;
    public final int blueScore;
    public final int timeRemainingSec;
    public final boolean matchOver;
    public final String winnerTeam;
    public final String lastEvent;

    public FootballStateSnapshot(String roomId, long serverTimestamp, BallSnapshot ball,
                                 List<PlayerSnapshot> players, int redScore, int blueScore,
                                 int timeRemainingSec, boolean matchOver, String winnerTeam, String lastEvent) {
        this.roomId = roomId;
        this.serverTimestamp = serverTimestamp;
        this.ball = ball;
        this.players = players;
        this.redScore = redScore;
        this.blueScore = blueScore;
        this.timeRemainingSec = timeRemainingSec;
        this.matchOver = matchOver;
        this.winnerTeam = winnerTeam;
        this.lastEvent = lastEvent;
    }

    public static class BallSnapshot {
        public final double x;
        public final double y;
        public final double vx;
        public final double vy;
        public final String carrierPlayerId;

        public BallSnapshot(double x, double y, double vx, double vy, String carrierPlayerId) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.carrierPlayerId = carrierPlayerId;
        }
    }

    public static class PlayerSnapshot {
        public final String playerId;
        public final String displayName;
        public final double x;
        public final double y;
        public final double vx;
        public final double vy;
        public final double aimAngleRad;
        public final String team;
        public final boolean hasBall;

        public PlayerSnapshot(String playerId, String displayName, double x, double y,
                              double vx, double vy, double aimAngleRad, String team, boolean hasBall) {
            this.playerId = playerId;
            this.displayName = displayName;
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.aimAngleRad = aimAngleRad;
            this.team = team;
            this.hasBall = hasBall;
        }
    }
}
