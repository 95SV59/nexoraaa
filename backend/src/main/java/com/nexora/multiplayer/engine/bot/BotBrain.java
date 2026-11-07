package com.nexora.multiplayer.engine.bot;

import com.nexora.multiplayer.engine.PlayerInput;

/**
 * Strategy interface for server-side bot AI. One implementation per game type.
 * computeInput() is called every game tick by GameLoopManager, after real-player
 * inputs have been processed and before updateState(). Bots never appear in
 * TelemetryCollector measurements — only real-player RTT/jitter/loss data feeds
 * the patent pipeline (see TelemetryCollector.onPongReceived).
 */
public interface BotBrain {

    /**
     * Given the current game state (as raw Object — the same snapshot that gets
     * broadcast to clients), compute and return the bot's intended input for this tick.
     *
     * @param botPlayerId  The bot's stable ID in the engine (e.g. "bot-1")
     * @param gameState    The result of game.getStateSnapshot() — cast to the appropriate
     *                     snapshot type for each game.
     * @return             A fully populated PlayerInput for this tick.
     */
    PlayerInput computeInput(String botPlayerId, Object gameState);
}
