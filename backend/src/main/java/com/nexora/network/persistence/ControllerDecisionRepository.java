package com.nexora.network.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ControllerDecisionRepository extends JpaRepository<ControllerDecisionEntity, Long> {
    List<ControllerDecisionEntity> findBySessionId(Long sessionId);
}
