package com.nexora.network.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WhatIfEvaluationRepository extends JpaRepository<WhatIfEvaluationEntity, Long> {
    List<WhatIfEvaluationEntity> findByCandidateIdIn(List<Long> candidateIds);
}
