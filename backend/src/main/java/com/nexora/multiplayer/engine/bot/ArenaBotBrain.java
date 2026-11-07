package com.nexora.multiplayer.engine.bot;

import com.nexora.multiplayer.engine.PlayerInput;
import com.nexora.multiplayer.engine.arena.ArenaStateSnapshot;

import java.util.List;
import java.util.Random;

/**
 * Bot AI for NEXORA Arena (2D shooter).
 * Strategy:
 *  - Find the nearest alive enemy.
 *  - Move toward them at full speed.
 *  - Aim at them and fire continuously.
 *  - Add small random jitter to aim so bots don't always hit (makes gameplay fair).
 */
public class ArenaBotBrain implements BotBrain {

    private static final double JITTER_RAD = 0.25; // ± 0.25 radians of aim noise
    private final Random rng = new Random();

    @Override
    public PlayerInput computeInput(String botPlayerId, Object gameState) {
        PlayerInput input = zeroInput();
        if (!(gameState instanceof ArenaStateSnapshot snap)) return input;

        ArenaStateSnapshot.PlayerSnapshot self = findSelf(snap.players, botPlayerId);
        if (self == null || !self.alive) return input;

        // Find nearest alive enemy
        ArenaStateSnapshot.PlayerSnapshot target = nearestAliveEnemy(snap.players, self);
        if (target == null) return input;

        double dx = target.x - self.x;
        double dy = target.y - self.y;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > 1e-3) {
            // Move toward enemy
            input.setMoveX(dx / dist);
            input.setMoveY(dy / dist);
        }

        // Aim at enemy + jitter
        double aimAngle = Math.atan2(dy, dx) + (rng.nextDouble() - 0.5) * JITTER_RAD;
        input.setAimAngleRad(aimAngle);
        input.setFiring(dist < 600); // only fire when somewhat close

        return input;
    }

    private ArenaStateSnapshot.PlayerSnapshot findSelf(List<ArenaStateSnapshot.PlayerSnapshot> players, String id) {
        return players.stream().filter(p -> id.equals(p.playerId)).findFirst().orElse(null);
    }

    private ArenaStateSnapshot.PlayerSnapshot nearestAliveEnemy(
            List<ArenaStateSnapshot.PlayerSnapshot> players,
            ArenaStateSnapshot.PlayerSnapshot self) {
        ArenaStateSnapshot.PlayerSnapshot nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (ArenaStateSnapshot.PlayerSnapshot p : players) {
            if (self.playerId.equals(p.playerId) || !p.alive) continue;
            double dx = p.x - self.x, dy = p.y - self.y;
            double d = dx * dx + dy * dy;
            if (d < bestDist) {
                bestDist = d;
                nearest = p;
            }
        }
        return nearest;
    }

    private PlayerInput zeroInput() {
        PlayerInput i = new PlayerInput();
        i.setMoveX(0);
        i.setMoveY(0);
        i.setAimAngleRad(0);
        i.setFiring(false);
        return i;
    }
}
