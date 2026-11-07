package com.nexora.multiplayer.engine.tag;

import com.nexora.multiplayer.engine.EnginePlayer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NexoraTagGameTest {

    private static NexoraTagGame newGame() {
        NexoraTagGame g = new NexoraTagGame();
        g.initialize("ROOM_TAG", List.of(new EnginePlayer("1", 1L, "Alice"), new EnginePlayer("2", 2L, "Bob")));
        return g;
    }

    @Test
    void exactlyOnePlayerStartsAsIt() {
        NexoraTagGame g = newGame();
        TagStateSnapshot snapshot = (TagStateSnapshot) g.getStateSnapshot();
        assertNotNull(snapshot.currentItPlayerId);
        long itCount = snapshot.players.stream().filter(p -> p.isIt).count();
        assertEquals(1, itCount);
        assertFalse(g.isMatchOver());
        assertTrue(g.validateState());
    }

    @Test
    void timeAccumulatesForItPlayer() {
        NexoraTagGame g = newGame();
        for (int i = 0; i < 30; i++) {
            g.updateState();
        }
        TagStateSnapshot snapshot = (TagStateSnapshot) g.getStateSnapshot();
        TagStateSnapshot.TagPlayerSnapshot itPlayer = snapshot.players.stream()
                .filter(p -> p.playerId.equals(snapshot.currentItPlayerId)).findFirst().orElseThrow();
        assertTrue(itPlayer.timeAsItSec > 0.5);
    }
}
