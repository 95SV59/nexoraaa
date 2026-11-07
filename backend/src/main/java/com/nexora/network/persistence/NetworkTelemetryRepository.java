package com.nexora.network.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NetworkTelemetryRepository extends JpaRepository<NetworkTelemetryEntity, Long> {
    List<NetworkTelemetryEntity> findBySessionId(Long sessionId);
}
