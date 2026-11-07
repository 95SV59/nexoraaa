package com.nexora.multiplayer.engine.racers;

import com.nexora.multiplayer.engine.EnginePlayer;
import com.nexora.multiplayer.engine.PlayerInput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NexoraRacersGameTest {

    private static NexoraRacersGame newGame() {
        NexoraRacersGame g = new NexoraRacersGame();
        g.initialize("ROOM_RACERS", List.of(new EnginePlayer("1", 1L, "Alice"), new EnginePlayer("2", 2L, "Bob")));
        return g;
    }

    private static PlayerInput throttleInput(String id, double moveY, double moveX) {
        PlayerInput in = new PlayerInput();
        in.setPlayerId(id);
        in.setMoveY(moveY);
        in.setMoveX(moveX);
        return in;
    }

    @Test
    void matchInitializesWithStartingLapAndPositions() {
        NexoraRacersGame g = newGame();
        RacersStateSnapshot snapshot = (RacersStateSnapshot) g.getStateSnapshot();
        assertEquals(2, snapshot.vehicles.size());
        assertEquals(1, snapshot.vehicles.get(0).lap);
        assertEquals(0, snapshot.vehicles.get(0).speed);
        assertFalse(g.isMatchOver());
        assertTrue(g.validateState());
    }

    @Test
    void throttleIncreasesSpeedAndAdvancesPosition() {
        NexoraRacersGame g = newGame();
        g.processInput("1", throttleInput("1", -1.0, 0.0)); // forward
        for (int i = 0; i < 30; i++) {
            g.updateState();
        }
        RacersStateSnapshot snapshot = (RacersStateSnapshot) g.getStateSnapshot();
        RacersStateSnapshot.VehicleSnapshot v = snapshot.vehicles.stream()
                .filter(veh -> veh.playerId.equals("1")).findFirst().orElseThrow();
        assertTrue(v.speed > 0);
        assertTrue(g.validateState());
    }

    @Test
    void metadataDeclaresExpectedConstraints() {
        NexoraRacersGame g = newGame();
        assertFalse(g.getStateMetadata().isEmpty());
        assertTrue(g.getStateMetadata().stream().anyMatch(m -> m.getElementName().equals("vehicle_position")));
        assertTrue(g.getStateMetadata().stream().anyMatch(m -> m.getElementName().equals("lap_checkpoint")));
    }
}
