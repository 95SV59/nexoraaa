package com.nexora.network.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PredictionErrorRepository extends JpaRepository<PredictionErrorEntity, Long> {
    List<PredictionErrorEntity> findByDecisionIdIn(List<Long> decisionIds);
}
