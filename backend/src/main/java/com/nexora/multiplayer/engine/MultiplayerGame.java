package com.nexora.multiplayer.engine;

import java.util.List;

/**
 * The common abstraction every multiplayer game implements, so the room/WebSocket/
 * (future) telemetry and network-controller infrastructure never needs to special-case
 * a specific game. See docs/ARCHITECTURE.md §3.
 *
 * Lifecycle: initialize() once when the match starts -> addPlayer() per player already
 * in the room -> repeating {processInput* then updateState() then broadcastState()} on
 * every tick, driven by GameLoopManager -> endMatch() once an end condition is reached.
 */
public interface MultiplayerGame {

    /** One-time setup for a new match. players is the full roster from the room. */
    void initialize(String roomId, List<EnginePlayer> players);

    /** A player joined after initialize() (e.g. late-join, if the game allows it). */
    void addPlayer(EnginePlayer player);

    /** A player disconnected or left; game decides how to handle it (bot takeover, forfeit, etc.). */
    void removePlayer(String playerId);

    /** Buffer one player's input for the next tick. Called as input arrives, off the tick clock. */
    void processInput(String playerId, PlayerInput input);

    /** Advance simulation by one tick: apply buffered inputs, move projectiles, resolve
     *  collisions, etc. Called at the engine's fixed tick rate. */
    void updateState();

    /** Server-authoritative sanity check (e.g. reject impossible positions) — return false
     *  to signal the tick's result should not be trusted/broadcast as-is. */
    boolean validateState();

    /** Serialize the current state into whatever the WebSocket layer will broadcast as
     *  GAME_STATE. Kept generic (Object) so each game defines its own state DTO shape. */
    Object getStateSnapshot();

    /** Declares per-state-element consistency requirements for this game — the input the
     *  (future) consistency/prediction engine reasons about generically. */
    List<StateElementMetadata> getStateMetadata();

    /** True once an end condition (score limit, time limit, last-player-standing, etc.)
     *  is reached; GameLoopManager checks this after every updateState(). */
    boolean isMatchOver();

    /** Server-computed final result. Called exactly once, when isMatchOver() first returns true. */
    MatchResultData endMatch();

    /** Simulation tick rate for this game. Arena (high-frequency movement/projectiles)
     *  overrides this upward; a turn-based game like Chess Arena could override it down. */
    default int getTickRateHz() { return 20; }
}
