package com.nexora.network.persistence;

import jakarta.persistence.*;

/** Maps to session_broadcast_stats (docs/DATABASE_DESIGN.md). session_id is the primary
 *  key directly (one row per GameSession) rather than a separate auto-generated id,
 *  since the relationship is strictly 1:1. This is the actual evidence a Phase 20
 *  comparison needs: total_broadcasts stays equal to total_ticks for a BASELINE run
 *  (never throttled) and can drop below it for PROPOSED under degraded conditions. */
@Entity
@Table(name = "session_broadcast_stats")
public class SessionBroadcastStatsEntity {

    @Id
    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "total_ticks", nullable = false)
    private long totalTicks;

    @Column(name = "total_broadcasts", nullable = false)
    private long totalBroadcasts;

    protected SessionBroadcastStatsEntity() { }

    public SessionBroadcastStatsEntity(Long sessionId, long totalTicks, long totalBroadcasts) {
        this.sessionId = sessionId;
        this.totalTicks = totalTicks;
        this.totalBroadcasts = totalBroadcasts;
    }

    public Long getSessionId() { return sessionId; }
    public long getTotalTicks() { return totalTicks; }
    public long getTotalBroadcasts() { return totalBroadcasts; }
}
