package com.nexora.multiplayer.engine;

/**
 * Declares how tolerant one named piece of a game's state is to staleness and
 * divergence. This is what lets the (future, Phase 12+) consistency/prediction
 * engine reason generically about any game, rather than hard-coding per-game
 * knowledge — see docs/ARCHITECTURE.md §3 and §6.
 */
public class StateElementMetadata {
    private final String elementName;
    private final StatePriority priority;
    private final double updateFrequencyHz;
    private final int maxStalenessMs;
    private final double maxDivergence;
    private final double maxRateOfChangePerSec;
    private final boolean predictionSupported;
    private final boolean compressionSupported;
    private final boolean aggregationSupported;

    /**
     * @param maxDivergence tolerated error, in the element's OWN units (pixels for a position,
     *                      radians for an aim angle, 0 for event-driven state).
     * @param maxRateOfChangePerSec how fast the element's value can change, in those SAME units
     *                      per second (e.g. 260 px/s for player position, ~4 rad/s for aim, 0 for
     *                      event-driven state). ConsistencyEngine multiplies this by predicted
     *                      jitter to estimate divergence, so both numbers must share units —
     *                      a single generic speed for every element gives nonsense for anything
     *                      that isn't a position (found the hard way: see docs/ARCHITECTURE.md §6).
     */
    public StateElementMetadata(String elementName, StatePriority priority, double updateFrequencyHz,
                                 int maxStalenessMs, double maxDivergence, double maxRateOfChangePerSec,
                                 boolean predictionSupported, boolean compressionSupported,
                                 boolean aggregationSupported) {
        this.elementName = elementName;
        this.priority = priority;
        this.updateFrequencyHz = updateFrequencyHz;
        this.maxStalenessMs = maxStalenessMs;
        this.maxDivergence = maxDivergence;
        this.maxRateOfChangePerSec = maxRateOfChangePerSec;
        this.predictionSupported = predictionSupported;
        this.compressionSupported = compressionSupported;
        this.aggregationSupported = aggregationSupported;
    }

    public String getElementName() { return elementName; }
    public StatePriority getPriority() { return priority; }
    public double getUpdateFrequencyHz() { return updateFrequencyHz; }
    public int getMaxStalenessMs() { return maxStalenessMs; }
    public double getMaxDivergence() { return maxDivergence; }
    public double getMaxRateOfChangePerSec() { return maxRateOfChangePerSec; }
    public boolean isPredictionSupported() { return predictionSupported; }
    public boolean isCompressionSupported() { return compressionSupported; }
    public boolean isAggregationSupported() { return aggregationSupported; }
}
