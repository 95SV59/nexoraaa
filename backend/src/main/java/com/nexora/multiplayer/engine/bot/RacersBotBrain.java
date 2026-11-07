package com.nexora.multiplayer.engine.bot;

import com.nexora.multiplayer.engine.PlayerInput;
import com.nexora.multiplayer.engine.racers.RacersConstants;
import com.nexora.multiplayer.engine.racers.RacersStateSnapshot;

/**
 * Bot AI for NEXORA Racers (kart racing).
 * Strategy:
 *  - Find own vehicle snapshot.
 *  - Steer toward the next checkpoint on the track.
 *  - Keep throttle at ~80% (not full speed so real players can catch/pass).
 *  - After finishing all laps, coast to zero throttle.
 */
public class RacersBotBrain implements BotBrain {

    private static final double BOT_THROTTLE = 0.8;

    @Override
    public PlayerInput computeInput(String botPlayerId, Object gameState) {
        PlayerInput input = zeroInput();
        if (!(gameState instanceof RacersStateSnapshot snap)) return input;

        RacersStateSnapshot.VehicleSnapshot self = snap.vehicles.stream()
                .filter(v -> botPlayerId.equals(v.playerId))
                .findFirst().orElse(null);

        if (self == null || self.finished) return input;

        int cpIdx = self.checkpoint % RacersConstants.CHECKPOINTS.length;
        double[] target = RacersConstants.CHECKPOINTS[cpIdx];
        double targetX = target[0], targetY = target[1];

        double dx = targetX - self.x;
        double dy = targetY - self.y;

        // Target heading angle
        double targetAngle = Math.atan2(dy, dx);

        // Current heading
        double currentAngle = self.headingAngleRad;

        // Normalize angle difference to [-pi, pi]
        double angleDiff = normalizeAngle(targetAngle - currentAngle);

        // Steer proportionally to angle difference (clamp to -1..1)
        double steer = Math.max(-1.0, Math.min(1.0, angleDiff * 2.0));

        input.setMoveX(steer);       // steer axis for racers (moveX)
        input.setMoveY(-BOT_THROTTLE); // throttle axis for racers (-moveY = forward)

        return input;
    }

    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
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
