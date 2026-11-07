package com.nexora.progression;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {
    List<LeaderboardEntry> findByGameSlugAndSeasonOrderByScoreDesc(String gameSlug, int season, Pageable pageable);
    Optional<LeaderboardEntry> findByGameSlugAndUserIdAndSeason(String gameSlug, Long userId, int season);

    @Query("SELECT l FROM LeaderboardEntry l WHERE l.gameSlug = :gameSlug AND l.season = :season AND l.user.id IN :userIds ORDER BY l.score DESC")
    List<LeaderboardEntry> findByGameSlugAndSeasonAndUserIdInOrderByScoreDesc(
            @Param("gameSlug") String gameSlug,
            @Param("season") int season,
            @Param("userIds") List<Long> userIds);
}
