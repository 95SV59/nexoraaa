package com.nexora.network.candidate;

/** The control strategies the candidate generator can propose (docs/ARCHITECTURE.md §5). */
public enum ActionType {
    NO_ACTION,
    REDUCE_LOW_PRIORITY_RATE,
    AGGREGATE_UPDATES,
    ENABLE_DEAD_RECKONING,
    PRIORITIZE_CRITICAL
}
