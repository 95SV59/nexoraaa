package com.nexora.multiplayer.engine.tag;

import com.nexora.multiplayer.engine.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * GAME 5 — NEXORA TAG. Fast-paced multiplayer tag for 2-10 players.
 * State: Player positions, Velocity, Tag state ("IT"), Immunity, Timer, Score (time as IT).
 * Patent-relevant surface: latency/jitter experiments — Tag state transfer is CRITICAL (50ms),
 * player positions are HIGH (70ms/5px).
 */
public class NexoraTagGame implements MultiplayerGame {

    private final Object lock = new Object();
    private final Map<String, TagPlayerState> players = new ConcurrentHashMap<>();

    private String roomId;
    private long matchStartMillis;
    private boolean matchOver = false;
    private String winnerPlayerId = null;
    private String currentItPlayerId = null;
    private String lastEvent = "TAG_START";

    @Override
    public void initialize(String roomId, List<EnginePlayer> roster) {
        synchronized (lock) {
            this.roomId = roomId;
            this.matchStartMillis = System.currentTimeMillis();
            this.matchOver = false;
            this.winnerPlayerId = null;
            this.lastEvent = "TAG_START";
            players.clear();

            int i = 0;
            for (EnginePlayer p : roster) {
                spawnPlayer(p, i++, roster.size());
            }

            // Pick the first player as IT
            if (!roster.isEmpty()) {
                currentItPlayerId = roster.get(0).getPlayerId();
                TagPlayerState first = players.get(currentItPlayerId);
                if (first != null) {
                    first.isIt = true;
                }
            }
        }
    }

    @Override
    public void addPlayer(EnginePlayer player) {
        synchronized (lock) {
            if (players.size() >= TagConstants.MAX_PLAYERS || matchOver) return;
            spawnPlayer(player, players.size(), Math.max(players.size() + 1, TagConstants.MIN_PLAYERS));
            if (currentItPlayerId == null) {
                currentItPlayerId = player.getPlayerId();
                TagPlayerState s = players.get(currentItPlayerId);
                if (s != null) s.isIt = true;
            }
        }
    }

    @Override
    public void removePlayer(String playerId) {
        synchronized (lock) {
            players.remove(playerId);
            if (playerId.equals(currentItPlayerId) && !players.isEmpty()) {
                currentItPlayerId = players.keySet().iterator().next();
                TagPlayerState next = players.get(currentItPlayerId);
                if (next != null) next.isIt = true;
            }
        }
    }

    @Override
    public void processInput(String playerId, PlayerInput input) {
        TagPlayerState p = players.get(playerId);
        if (p == null) return;
        synchronized (lock) {
            p.pendingMoveX = clamp(input.getMoveX(), -1.0, 1.0);
            p.pendingMoveY = clamp(input.getMoveY(), -1.0, 1.0);
        }
    }

    @Override
    public void updateState() {
        synchronized (lock) {
            if (matchOver) return;
            double dt = 1.0 / TagConstants.TICK_RATE_HZ;
            long now = System.currentTimeMillis();

            int timeRemaining = Math.max(0, TagConstants.MATCH_DURATION_SEC - (int) ((now - matchStartMillis) / 1000));
            if (timeRemaining <= 0) {
                matchOver = true;
                // Winner is player with least time spent as IT
                winnerPlayerId = players.values().stream()
                        .min(Comparator.comparingDouble(p -> p.timeAsItSec))
                        .map(p -> p.player.getPlayerId())
                        .orElse(null);
                return;
            }

            TagPlayerState itPlayer = currentItPlayerId != null ? players.get(currentItPlayerId) : null;
            if (itPlayer != null) {
                itPlayer.timeAsItSec += dt;
            }

            // Move players
            for (TagPlayerState p : players.values()) {
                double speed = p.isIt ? TagConstants.IT_SPEED : TagConstants.RUNNER_SPEED;
                p.vx = p.pendingMoveX * speed;
                p.vy = p.pendingMoveY * speed;

                p.x = clamp(p.x + p.vx * dt, 30.0, TagConstants.ARENA_WIDTH - 30.0);
                p.y = clamp(p.y + p.vy * dt, 30.0, TagConstants.ARENA_HEIGHT - 30.0);
            }

            // Check tag transfer
            if (itPlayer != null && now > itPlayer.immuneUntilMs) {
                for (TagPlayerState runner : players.values()) {
                    if (runner == itPlayer || now <= runner.immuneUntilMs) continue;

                    double dSq = distSq(itPlayer.x, itPlayer.y, runner.x, runner.y);
                    if (dSq <= TagConstants.TAG_DISTANCE * TagConstants.TAG_DISTANCE) {
                        // Tag successful!
                        itPlayer.isIt = false;
                        itPlayer.immuneUntilMs = now + TagConstants.TAG_IMMUNITY_MS;
                        itPlayer.tagsMade++;

                        runner.isIt = true;
                        runner.immuneUntilMs = now + TagConstants.TAG_IMMUNITY_MS;
                        currentItPlayerId = runner.player.getPlayerId();
                        lastEvent = "TAGGED_" + runner.player.getDisplayName();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean validateState() {
        for (TagPlayerState p : players.values()) {
            if (Double.isNaN(p.x) || Double.isNaN(p.y)) return false;
        }
        return true;
    }

    @Override
    public Object getStateSnapshot() {
        synchronized (lock) {
            long now = System.currentTimeMillis();
            int timeRemaining = Math.max(0, TagConstants.MATCH_DURATION_SEC - (int) ((now - matchStartMillis) / 1000));

            List<TagStateSnapshot.TagPlayerSnapshot> list = players.values().stream()
                    .map(p -> new TagStateSnapshot.TagPlayerSnapshot(
                            p.player.getPlayerId(),
                            p.player.getDisplayName(),
                            round(p.x),
                            round(p.y),
                            round(p.vx),
                            round(p.vy),
                            p.isIt,
                            round(p.timeAsItSec),
                            now <= p.immuneUntilMs
                    ))
                    .collect(Collectors.toList());

            return new TagStateSnapshot(
                    roomId,
                    now,
                    list,
                    currentItPlayerId,
                    timeRemaining,
                    matchOver,
                    winnerPlayerId,
                    lastEvent
            );
        }
    }

    @Override
    public List<StateElementMetadata> getStateMetadata() {
        return List.of(
                new StateElementMetadata("player_position", StatePriority.HIGH, 30.0, 70, 5.0, 225.0, true, true, true),
                new StateElementMetadata("tag_state", StatePriority.CRITICAL, 20.0, 50, 0.0, 1.0, false, false, false),
                new StateElementMetadata("match_timer", StatePriority.MODERATE, 1.0, 500, 1.0, 1.0, false, false, false)
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
            List<Map<String, Object>> standings = players.values().stream()
                    .sorted(Comparator.comparingDouble(p -> p.timeAsItSec))
                    .map(p -> Map.<String, Object>of(
                            "playerId", p.player.getPlayerId(),
                            "displayName", p.player.getDisplayName(),
                            "timeAsItSec", round(p.timeAsItSec),
                            "tagsMade", p.tagsMade
                    ))
                    .collect(Collectors.toList());

            summary.put("game", "tag");
            summary.put("standings", standings);
            return new MatchResultData(winnerPlayerId, durationSec, summary);
        }
    }

    @Override
    public int getTickRateHz() {
        return TagConstants.TICK_RATE_HZ;
    }

    private void spawnPlayer(EnginePlayer p, int index, int total) {
        // Place players in circle around arena center
        double angle = 2.0 * Math.PI * index / Math.max(1, total);
        double radius = 180.0;
        double cx = TagConstants.ARENA_WIDTH / 2.0;
        double cy = TagConstants.ARENA_HEIGHT / 2.0;
        double startX = cx + radius * Math.cos(angle);
        double startY = cy + radius * Math.sin(angle);
        players.put(p.getPlayerId(), new TagPlayerState(p, startX, startY));
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
