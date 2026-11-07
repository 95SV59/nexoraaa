package com.nexora.multiplayer.engine;

/** Matches the priority column of game_state_metric (docs/DATABASE_DESIGN.md). */
public enum StatePriority {
    CRITICAL,
    HIGH,
    MODERATE,
    LOW,
    VERY_LOW
}
