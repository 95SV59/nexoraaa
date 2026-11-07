package com.nexora.network.experiment;

import com.nexora.multiplayer.session.GameSession;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 20 — lets a room be prepared for a specific controller mode before its match
 * starts, so the same NetworkLabProfile can be run once as BASELINE and once as
 * PROPOSED for a fair comparison (see ExperimentAnalysisService). Set via
 * CreateRoomRequest.controllerMode at room-creation time; read once, when
 * GameLoopManager.startMatch() creates the GameSession.
 *
 * Same "dumb per-room registry" pattern as NetworkLabService/BroadcastRateRegistry, for
 * the same reason: it keeps this operational, session-scoped setting out of the JPA
 * entity graph entirely, so it never has to be reconciled against database.schema.sql.
 */
@Component
public class ControllerModeRegistry {

    private final Map<String, GameSession.ControllerMode> modes = new ConcurrentHashMap<>();

    public void setMode(String roomCode, GameSession.ControllerMode mode) {
        modes.put(roomCode, mode == null ? GameSession.ControllerMode.PROPOSED : mode);
    }

    public GameSession.ControllerMode getMode(String roomCode) {
        return modes.getOrDefault(roomCode, GameSession.ControllerMode.PROPOSED);
    }

    public void clear(String roomCode) {
        modes.remove(roomCode);
    }
}
