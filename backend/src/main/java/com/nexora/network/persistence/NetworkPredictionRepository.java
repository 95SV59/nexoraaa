package com.nexora.network.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NetworkPredictionRepository extends JpaRepository<NetworkPredictionEntity, Long> {
    List<NetworkPredictionEntity> findBySessionId(Long sessionId);
}
