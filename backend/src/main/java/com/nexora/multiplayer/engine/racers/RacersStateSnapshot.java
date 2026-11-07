package com.nexora.multiplayer.engine.racers;

import java.util.List;

public class RacersStateSnapshot {
    public final String gameId = "racers";
    public final String roomId;
    public final long serverTimestamp;
    public final List<VehicleSnapshot> vehicles;
    public final boolean matchOver;
    public final String winnerPlayerId;
    public final long elapsedMs;

    public RacersStateSnapshot(String roomId, long serverTimestamp, List<VehicleSnapshot> vehicles,
                               boolean matchOver, String winnerPlayerId, long elapsedMs) {
        this.roomId = roomId;
        this.serverTimestamp = serverTimestamp;
        this.vehicles = vehicles;
        this.matchOver = matchOver;
        this.winnerPlayerId = winnerPlayerId;
        this.elapsedMs = elapsedMs;
    }

    public static class VehicleSnapshot {
        public final String playerId;
        public final String displayName;
        public final double x;
        public final double y;
        public final double vx;
        public final double vy;
        public final double headingAngleRad;
        public final double speed;
        public final int lap;
        public final int checkpoint;
        public final boolean finished;
        public final int place;

        public VehicleSnapshot(String playerId, String displayName, double x, double y,
                               double vx, double vy, double headingAngleRad, double speed,
                               int lap, int checkpoint, boolean finished, int place) {
            this.playerId = playerId;
            this.displayName = displayName;
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.headingAngleRad = headingAngleRad;
            this.speed = speed;
            this.lap = lap;
            this.checkpoint = checkpoint;
            this.finished = finished;
            this.place = place;
        }
    }
}
