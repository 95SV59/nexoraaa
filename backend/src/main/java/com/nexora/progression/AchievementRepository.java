package com.nexora.progression;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    Optional<Achievement> findBySlug(String slug);
    List<Achievement> findByGameSlug(String gameSlug);
    List<Achievement> findByGameSlugIsNull();
    List<Achievement> findByCriteriaType(String criteriaType);
}
