package com.nexora.multiplayer.engine.football;

import com.nexora.multiplayer.engine.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * GAME 4 — NEXORA FOOTBALL. Arcade multiplayer football for 2-6 players.
 * State: Player position, Ball position, Velocity, Possession, Tackles, Goals, Match timer.
 * Patent-relevant surface: state-specific consistency constraints — Ball Position is CRITICAL
 * with tight tolerance (60ms/4px), Player Position is HIGH (90ms/8px), Score is CRITICAL,
 * while cosmetic crowd animation is VERY_LOW.
 */
public class NexoraFootballGame implements MultiplayerGame {

    private final Object lock = new Object();
    private final Map<String, FootballPlayerState> players = new ConcurrentHashMap<>();

    private double ballX = 450.0;
    private double ballY = 275.0;
    private double ballVx = 0.0;
    private double ballVy = 0.0;
    private String ballCarrierId = null;

    private int redScore = 0;
    private int blueScore = 0;
    private String roomId;
    private long matchStartMillis;
    private boolean matchOver = false;
    private String winnerTeam = null;
    private String lastEvent = "KICKOFF";

    @Override
    public void initialize(String roomId, List<EnginePlayer> roster) {
        synchronized (lock) {
            this.roomId = roomId;
            this.matchStartMillis = System.currentTimeMillis();
            this.matchOver = false;
            this.winnerTeam = null;
            this.redScore = 0;
            this.blueScore = 0;
            this.lastEvent = "KICKOFF";
            players.clear();

            int i = 0;
            for (EnginePlayer p : roster) {
                spawnPlayer(p, i++, roster.size());
            }
            resetBall();
        }
    }

    @Override
    public void addPlayer(EnginePlayer player) {
        synchronized (lock) {
            if (players.size() >= FootballConstants.MAX_PLAYERS || matchOver) return;
            spawnPlayer(player, players.size(), Math.max(players.size() + 1, FootballConstants.MIN_PLAYERS));
        }
    }

    @Override
    public void removePlayer(String playerId) {
        synchronized (lock) {
            FootballPlayerState removed = players.remove(playerId);
            if (removed != null && playerId.equals(ballCarrierId)) {
                ballCarrierId = null;
            }
        }
    }

    @Override
    public void processInput(String playerId, PlayerInput input) {
        FootballPlayerState p = players.get(playerId);
        if (p == null) return;
        synchronized (lock) {
            p.pendingMoveX = clamp(input.getMoveX(), -1.0, 1.0);
            p.pendingMoveY = clamp(input.getMoveY(), -1.0, 1.0);
            p.aimAngleRad = input.getAimAngleRad();
            p.pendingKick = input.isFiring();
        }
    }

    @Override
    public void updateState() {
        synchronized (lock) {
            if (matchOver) return;
            double dt = 1.0 / FootballConstants.TICK_RATE_HZ;
            long now = System.currentTimeMillis();

            int timeRemaining = Math.max(0, FootballConstants.MATCH_DURATION_SEC - (int) ((now - matchStartMillis) / 1000));
            if (timeRemaining <= 0 || redScore >= FootballConstants.SCORE_LIMIT || blueScore >= FootballConstants.SCORE_LIMIT) {
                matchOver = true;
                if (redScore > blueScore) winnerTeam = "RED";
                else if (blueScore > redScore) winnerTeam = "BLUE";
                else winnerTeam = "DRAW";
                return;
            }

            // Move players
            for (FootballPlayerState p : players.values()) {
                p.vx = p.pendingMoveX * FootballConstants.PLAYER_SPEED;
                p.vy = p.pendingMoveY * FootballConstants.PLAYER_SPEED;

                p.x = clamp(p.x + p.vx * dt, 40.0, FootballConstants.PITCH_WIDTH - 40.0);
                p.y = clamp(p.y + p.vy * dt, 40.0, FootballConstants.PITCH_HEIGHT - 40.0);

                // Handle kick if carrier
                if (p.player.getPlayerId().equals(ballCarrierId)) {
                    if (p.pendingKick) {
                        ballCarrierId = null;
                        p.hasBall = false;
                        p.tackleCooldownUntil = now + 400; // prevent immediate recapture
                        ballVx = FootballConstants.KICK_SPEED * Math.cos(p.aimAngleRad);
                        ballVy = FootballConstants.KICK_SPEED * Math.sin(p.aimAngleRad);
                        lastEvent = "KICK_" + p.team;
                    }
                }
            }

            // Ball physics
            if (ballCarrierId != null) {
                FootballPlayerState carrier = players.get(ballCarrierId);
                if (carrier != null) {
                    carrier.hasBall = true;
                    // Ball slightly in front of carrier
                    ballX = carrier.x + 22.0 * Math.cos(carrier.aimAngleRad);
                    ballY = carrier.y + 22.0 * Math.sin(carrier.aimAngleRad);
                    ballVx = carrier.vx;
                    ballVy = carrier.vy;

                    // Check for tackles by opposing team
                    for (FootballPlayerState defender : players.values()) {
                        if (!defender.team.equals(carrier.team) && now > defender.tackleCooldownUntil) {
                            double distSq = distSq(defender.x, defender.y, carrier.x, carrier.y);
                            if (distSq < (FootballConstants.PLAYER_RADIUS * 2) * (FootballConstants.PLAYER_RADIUS * 2)) {
                                // Tackle successful!
                                ballCarrierId = defender.player.getPlayerId();
                                carrier.hasBall = false;
                                defender.hasBall = true;
                                carrier.tackleCooldownUntil = now + 600;
                                lastEvent = "TACKLE_" + defender.team;
                                break;
                            }
                        }
                    }
                } else {
                    ballCarrierId = null;
                }
            } else {
                // Ball is loose: advance and apply friction
                ballX += ballVx * dt;
                ballY += ballVy * dt;

                double speed = Math.sqrt(ballVx * ballVx + ballVy * ballVy);
                if (speed > 0.1) {
                    double newSpeed = Math.max(0, speed - FootballConstants.BALL_FRICTION * dt);
                    ballVx = (ballVx / speed) * newSpeed;
                    ballVy = (ballVy / speed) * newSpeed;
                } else {
                    ballVx = 0;
                    ballVy = 0;
                }

                // Bounce off top and bottom walls
                if (ballY < 40.0) {
                    ballY = 40.0;
                    ballVy = -ballVy * 0.7;
                } else if (ballY > FootballConstants.PITCH_HEIGHT - 40.0) {
                    ballY = FootballConstants.PITCH_HEIGHT - 40.0;
                    ballVy = -ballVy * 0.7;
                }

                // Check Goal Left
                if (ballX <= FootballConstants.GOAL_LINE_LEFT) {
                    if (ballY >= FootballConstants.GOAL_Y_MIN && ballY <= FootballConstants.GOAL_Y_MAX) {
                        redScore++;
                        lastEvent = "GOAL_RED";
                        resetPositions();
                        return;
                    } else {
                        ballX = FootballConstants.GOAL_LINE_LEFT;
                        ballVx = -ballVx * 0.7;
                    }
                }

                // Check Goal Right
                if (ballX >= FootballConstants.GOAL_LINE_RIGHT) {
                    if (ballY >= FootballConstants.GOAL_Y_MIN && ballY <= FootballConstants.GOAL_Y_MAX) {
                        blueScore++;
                        lastEvent = "GOAL_BLUE";
                        resetPositions();
                        return;
                    } else {
                        ballX = FootballConstants.GOAL_LINE_RIGHT;
                        ballVx = -ballVx * 0.7;
                    }
                }

                // Interception / pickup by nearest player
                for (FootballPlayerState p : players.values()) {
                    if (now > p.tackleCooldownUntil) {
                        double dSq = distSq(p.x, p.y, ballX, ballY);
                        if (dSq < (FootballConstants.PLAYER_RADIUS + FootballConstants.BALL_RADIUS) * (FootballConstants.PLAYER_RADIUS + FootballConstants.BALL_RADIUS)) {
                            ballCarrierId = p.player.getPlayerId();
                            p.hasBall = true;
                            lastEvent = "PICKUP_" + p.team;
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean validateState() {
        if (Double.isNaN(ballX) || Double.isNaN(ballY)) return false;
        for (FootballPlayerState p : players.values()) {
            if (Double.isNaN(p.x) || Double.isNaN(p.y)) return false;
        }
        return true;
    }

    @Override
    public Object getStateSnapshot() {
        synchronized (lock) {
            long now = System.currentTimeMillis();
            int timeRemaining = Math.max(0, FootballConstants.MATCH_DURATION_SEC - (int) ((now - matchStartMillis) / 1000));

            FootballStateSnapshot.BallSnapshot ballSnap = new FootballStateSnapshot.BallSnapshot(
                    round(ballX), round(ballY), round(ballVx), round(ballVy), ballCarrierId);

            List<FootballStateSnapshot.PlayerSnapshot> playerSnaps = players.values().stream()
                    .map(p -> new FootballStateSnapshot.PlayerSnapshot(
                            p.player.getPlayerId(),
                            p.player.getDisplayName(),
                            round(p.x),
                            round(p.y),
                            round(p.vx),
                            round(p.vy),
                            round(p.aimAngleRad),
                            p.team,
                            p.player.getPlayerId().equals(ballCarrierId)
                    ))
                    .collect(Collectors.toList());

            return new FootballStateSnapshot(
                    roomId,
                    now,
                    ballSnap,
                    playerSnaps,
                    redScore,
                    blueScore,
                    timeRemaining,
                    matchOver,
                    winnerTeam,
                    lastEvent
            );
        }
    }

    @Override
    public List<StateElementMetadata> getStateMetadata() {
        return List.of(
                new StateElementMetadata("ball_position", StatePriority.CRITICAL, 30.0, 60, 4.0, 380.0, true, true, true),
                new StateElementMetadata("player_position", StatePriority.HIGH, 25.0, 90, 8.0, 180.0, true, true, true),
                new StateElementMetadata("match_score", StatePriority.CRITICAL, 5.0, 100, 0.0, 1.0, false, false, false),
                new StateElementMetadata("crowd_animation", StatePriority.VERY_LOW, 5.0, 500, 50.0, 10.0, false, false, false)
        );
    }

    @Override
    public boolean isMatchOver() {
        return matchOver;
    }

    @Override
    public MatchResultData endMatch() {
        synchronized (lock) {
            this.matchOver = true;
            int durationSec = (int) Math.max(1, (System.currentTimeMillis() - matchStartMillis) / 1000);
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("game", "football");
            summary.put("redScore", redScore);
            summary.put("blueScore", blueScore);
            summary.put("winnerTeam", winnerTeam);

            // Winner player id if any (first player of winning team)
            String winnerPlayerId = players.values().stream()
                    .filter(p -> p.team.equals(winnerTeam))
                    .map(p -> p.player.getPlayerId())
                    .findFirst().orElse(null);

            return new MatchResultData(winnerPlayerId, durationSec, summary);
        }
    }

    @Override
    public int getTickRateHz() {
        return FootballConstants.TICK_RATE_HZ;
    }

    private void resetBall() {
        ballX = FootballConstants.PITCH_WIDTH / 2.0;
        ballY = FootballConstants.PITCH_HEIGHT / 2.0;
        ballVx = 0.0;
        ballVy = 0.0;
        ballCarrierId = null;
    }

    private void resetPositions() {
        resetBall();
        int i = 0;
        for (FootballPlayerState p : players.values()) {
            double startX = p.team.equals("RED") ? 300.0 : 600.0;
            double startY = 180.0 + (i % 3) * 90.0;
            p.x = startX;
            p.y = startY;
            p.vx = 0;
            p.vy = 0;
            p.hasBall = false;
            i++;
        }
    }

    private void spawnPlayer(EnginePlayer p, int index, int total) {
        String team = (index % 2 == 0) ? "RED" : "BLUE";
        double startX = team.equals("RED") ? 300.0 : 600.0;
        double startY = 180.0 + (index / 2) * 90.0;
        players.put(p.getPlayerId(), new FootballPlayerState(p, startX, startY, team));
    }

    private static double distSq(double x1, double y1, double x2, double y2) {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
