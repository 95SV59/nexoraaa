package com.nexora.network.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionBroadcastStatsRepository extends JpaRepository<SessionBroadcastStatsEntity, Long> {
    // findById(sessionId) from JpaRepository is exactly what's needed — session_id IS the PK.
}
