package com.nexora.multiplayer.engine.bot;

import com.nexora.multiplayer.engine.PlayerInput;
import com.nexora.multiplayer.engine.football.FootballStateSnapshot;

import java.util.List;
import java.util.Random;

/**
 * Bot AI for NEXORA Football (arcade soccer).
 * Strategy:
 *  - If team controls ball: carrier moves toward opposing goal, teammates support.
 *  - If opposing team has ball: chase the carrier and attempt tackle (move directly into them).
 *  - If ball is loose: nearest bot on each side chases the ball.
 *  - Kick in direction of goal when holding the ball.
 *  Career bots play at ~70% speed so human players can outmaneuver them.
 */
public class FootballBotBrain implements BotBrain {

    // Approximate goal X positions (from FootballConstants)
    private static final double GOAL_LEFT_X = 0.0;
    private static final double GOAL_RIGHT_X = 900.0;
    private static final double GOAL_CENTER_Y = 275.0;
    private static final double BOT_SPEED_FACTOR = 0.85; // move input is normalized 0..1

    private final Random rng = new Random();

    @Override
    public PlayerInput computeInput(String botPlayerId, Object gameState) {
        PlayerInput input = zeroInput();
        if (!(gameState instanceof FootballStateSnapshot snap)) return input;

        FootballStateSnapshot.PlayerSnapshot self = findSelf(snap.players, botPlayerId);
        if (self == null) return input;

        FootballStateSnapshot.BallSnapshot ball = snap.ball;

        // Determine bot's attacking goal (opposite side)
        double attackGoalX = self.team.equals("RED") ? GOAL_RIGHT_X : GOAL_LEFT_X;

        boolean botHasBall = botPlayerId.equals(ball.carrierPlayerId);
        boolean teamHasBall = ball.carrierPlayerId != null && teamOf(snap.players, ball.carrierPlayerId, self.team);

        if (botHasBall) {
            // Move toward and kick at opposing goal
            double dx = attackGoalX - self.x;
            double dy = GOAL_CENTER_Y - self.y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > 1e-3) {
                input.setMoveX((dx / dist) * BOT_SPEED_FACTOR);
                input.setMoveY((dy / dist) * BOT_SPEED_FACTOR);
            }
            double aimAngle = Math.atan2(GOAL_CENTER_Y - self.y, attackGoalX - self.x);
            input.setAimAngleRad(aimAngle);
            input.setFiring(dist < 250); // kick when close to goal
        } else if (ball.carrierPlayerId != null && !teamHasBall) {
            // Opponent has ball: chase carrier to tackle
            FootballStateSnapshot.PlayerSnapshot carrier = findSelf(snap.players, ball.carrierPlayerId);
            if (carrier != null) {
                double dx = carrier.x - self.x;
                double dy = carrier.y - self.y;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 1e-3) {
                    input.setMoveX((dx / dist) * BOT_SPEED_FACTOR);
                    input.setMoveY((dy / dist) * BOT_SPEED_FACTOR);
                }
            }
        } else {
            // Ball is loose or teammate has ball: move toward ball
            double dx = ball.x - self.x;
            double dy = ball.y - self.y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > 1e-3) {
                input.setMoveX((dx / dist) * BOT_SPEED_FACTOR);
                input.setMoveY((dy / dist) * BOT_SPEED_FACTOR);
            }
        }

        return input;
    }

    private FootballStateSnapshot.PlayerSnapshot findSelf(List<FootballStateSnapshot.PlayerSnapshot> players, String id) {
        return players.stream().filter(p -> id.equals(p.playerId)).findFirst().orElse(null);
    }

    private boolean teamOf(List<FootballStateSnapshot.PlayerSnapshot> players, String playerId, String myTeam) {
        return players.stream()
                .filter(p -> playerId.equals(p.playerId))
                .anyMatch(p -> myTeam.equals(p.team));
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
