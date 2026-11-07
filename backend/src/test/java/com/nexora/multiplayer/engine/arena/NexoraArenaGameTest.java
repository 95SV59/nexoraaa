package com.nexora.multiplayer.engine.arena;

import com.nexora.multiplayer.engine.EnginePlayer;
import com.nexora.multiplayer.engine.PlayerInput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NexoraArenaGameTest {

    private static NexoraArenaGame newGame() {
        NexoraArenaGame g = new NexoraArenaGame();
        g.initialize("ROOM", List.of(new EnginePlayer("1", 1L, "Ann"), new EnginePlayer("2", 2L, "Bob")));
        return g;
    }

    private static PlayerInput input(String id, double mx, double my, double aim, boolean fire) {
        PlayerInput in = new PlayerInput();
        in.setPlayerId(id);
        in.setMoveX(mx);
        in.setMoveY(my);
        in.setAimAngleRad(aim);
        in.setFiring(fire);
        return in;
    }

    private static ArenaStateSnapshot.PlayerSnapshot player(NexoraArenaGame g, String id) {
        ArenaStateSnapshot s = (ArenaStateSnapshot) g.getStateSnapshot();
        return s.players.stream().filter(p -> p.playerId.equals(id)).findFirst().orElseThrow();
    }

    @Test
    void newMatchStartsWithTwoFullHealthPlayersAndNoScore() {
        NexoraArenaGame g = newGame();
        assertEquals(2, ((ArenaStateSnapshot) g.getStateSnapshot()).players.size());
        assertEquals(ArenaConstants.PLAYER_MAX_HEALTH, player(g, "1").health);
        assertEquals(0, player(g, "1").score);
        assertFalse(g.isMatchOver());
        assertTrue(g.validateState());
    }

    @Test
    void playerCannotLeaveTheArena() {
        NexoraArenaGame g = newGame();
        g.processInput("1", input("1", 1, 0, 0, false)); // hold right for far longer than the arena is wide
        for (int i = 0; i < 600; i++) g.updateState();
        assertEquals(ArenaConstants.ARENA_WIDTH - ArenaConstants.PLAYER_RADIUS, player(g, "1").x, 0.001);
        assertTrue(g.validateState());
    }

    @Test
    void aProjectileThatHitsDamagesTheTarget() {
        NexoraArenaGame g = newGame();
        // Player 1 spawns to the right of centre, player 2 to the left; aim straight at them (angle = pi).
        g.processInput("1", input("1", 0, 0, Math.PI, true));
        g.updateState();                                   // fires exactly one projectile
        g.processInput("1", input("1", 0, 0, Math.PI, false)); // release before the cooldown allows a second
        for (int i = 0; i < 90 && player(g, "2").health == ArenaConstants.PLAYER_MAX_HEALTH; i++) g.updateState();
        assertEquals(ArenaConstants.PLAYER_MAX_HEALTH - ArenaConstants.PROJECTILE_DAMAGE, player(g, "2").health);
    }

    @Test
    void metadataDeclaresUnitsForEveryContinuousElement() {
        new NexoraArenaGame().getStateMetadata().forEach(m -> {
            if (m.getUpdateFrequencyHz() > 0 && m.getMaxDivergence() > 0) {
                assertTrue(m.getMaxRateOfChangePerSec() > 0,
                        m.getElementName() + " has a divergence tolerance but no rate of change to estimate against");
            }
        });
    }
}
