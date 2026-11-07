package com.nexora.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByGameIdOrderByCreatedAtDesc(Long gameId);
    List<Review> findByGameSlugOrderByCreatedAtDesc(String slug);
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Review> findByReportedTrueOrderByCreatedAtDesc();
    long countByGameId(Long gameId);
    long countByUserId(Long userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.game.id = :gameId")
    Double getAverageRatingForGame(@Param("gameId") Long gameId);
}
