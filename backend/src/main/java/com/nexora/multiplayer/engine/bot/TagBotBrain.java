package com.nexora.multiplayer.engine.bot;

import com.nexora.multiplayer.engine.PlayerInput;
import com.nexora.multiplayer.engine.tag.TagStateSnapshot;

import java.util.List;
import java.util.Random;

/**
 * Bot AI for NEXORA Tag.
 * Strategy:
 *  - If THIS bot is IT: move directly toward the nearest non-immune runner.
 *  - If THIS bot is a runner: flee away from the IT player at full speed.
 *  - Small random drift added so bots don't perfectly predict each other.
 */
public class TagBotBrain implements BotBrain {

    private static final double BOT_SPEED_FACTOR = 0.9;
    private static final double DRIFT = 0.15; // random directional noise
    private final Random rng = new Random();

    @Override
    public PlayerInput computeInput(String botPlayerId, Object gameState) {
        PlayerInput input = zeroInput();
        if (!(gameState instanceof TagStateSnapshot snap)) return input;

        TagStateSnapshot.TagPlayerSnapshot self = findSelf(snap.players, botPlayerId);
        if (self == null) return input;

        if (self.isIt) {
            // Chase nearest non-immune runner
            TagStateSnapshot.TagPlayerSnapshot target = nearestRunner(snap.players, botPlayerId);
            if (target != null) {
                double dx = target.x - self.x;
                double dy = target.y - self.y;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 1e-3) {
                    input.setMoveX(clamp((dx / dist + randomDrift()) * BOT_SPEED_FACTOR, -1, 1));
                    input.setMoveY(clamp((dy / dist + randomDrift()) * BOT_SPEED_FACTOR, -1, 1));
                }
            }
        } else {
            // Run away from IT player
            TagStateSnapshot.TagPlayerSnapshot itPlayer = findSelf(snap.players, snap.currentItPlayerId);
            if (itPlayer != null) {
                double dx = self.x - itPlayer.x; // flee direction
                double dy = self.y - itPlayer.y;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 1e-3) {
                    input.setMoveX(clamp((dx / dist + randomDrift()) * BOT_SPEED_FACTOR, -1, 1));
                    input.setMoveY(clamp((dy / dist + randomDrift()) * BOT_SPEED_FACTOR, -1, 1));
                } else {
                    // IT is on top of us: random direction
                    input.setMoveX((rng.nextDouble() * 2 - 1));
                    input.setMoveY((rng.nextDouble() * 2 - 1));
                }
            }
        }

        return input;
    }

    private TagStateSnapshot.TagPlayerSnapshot findSelf(List<TagStateSnapshot.TagPlayerSnapshot> players, String id) {
        if (id == null) return null;
        return players.stream().filter(p -> id.equals(p.playerId)).findFirst().orElse(null);
    }

    private TagStateSnapshot.TagPlayerSnapshot nearestRunner(List<TagStateSnapshot.TagPlayerSnapshot> players, String itId) {
        TagStateSnapshot.TagPlayerSnapshot self = findSelf(players, itId);
        if (self == null) return null;
        TagStateSnapshot.TagPlayerSnapshot nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (TagStateSnapshot.TagPlayerSnapshot p : players) {
            if (itId.equals(p.playerId) || p.immune) continue;
            double dx = p.x - self.x, dy = p.y - self.y;
            double d = dx * dx + dy * dy;
            if (d < bestDist) {
                bestDist = d;
                nearest = p;
            }
        }
        return nearest;
    }

    private double randomDrift() {
        return (rng.nextDouble() - 0.5) * DRIFT;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
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
