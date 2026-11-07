package com.nexora.multiplayer.engine.football;

import com.nexora.multiplayer.engine.EnginePlayer;
import com.nexora.multiplayer.engine.PlayerInput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NexoraFootballGameTest {

    private static NexoraFootballGame newGame() {
        NexoraFootballGame g = new NexoraFootballGame();
        g.initialize("ROOM_FOOTBALL", List.of(new EnginePlayer("1", 1L, "Red1"), new EnginePlayer("2", 2L, "Blue1")));
        return g;
    }

    @Test
    void ballInitializesAtCenterAndScoresAtZero() {
        NexoraFootballGame g = newGame();
        FootballStateSnapshot snapshot = (FootballStateSnapshot) g.getStateSnapshot();
        assertEquals(450.0, snapshot.ball.x, 1.0);
        assertEquals(275.0, snapshot.ball.y, 1.0);
        assertEquals(0, snapshot.redScore);
        assertEquals(0, snapshot.blueScore);
        assertFalse(g.isMatchOver());
        assertTrue(g.validateState());
    }

    @Test
    void metadataDeclaresBallAsCriticalPriority() {
        NexoraFootballGame g = newGame();
        assertTrue(g.getStateMetadata().stream()
                .anyMatch(m -> m.getElementName().equals("ball_position") && m.getPriority().name().equals("CRITICAL")));
    }
}
