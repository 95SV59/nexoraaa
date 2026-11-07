package com.nexora.game;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findBySlug(String slug);

    /**
     * One query handles every combination the catalogue UI needs (search text, genre,
     * platform, multiplayer-only) via JPQL's `(:param IS NULL OR ...)` pattern — simpler to
     * reason about than a Specification builder for this small a set of filters, and sorting
     * comes for free from the Pageable's Sort.
     */
    @Query("""
            SELECT DISTINCT g FROM Game g
            LEFT JOIN g.platforms p
            WHERE (:search IS NULL OR LOWER(g.title) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:genreId IS NULL OR g.genre.id = :genreId)
              AND (:platformId IS NULL OR p.id = :platformId)
              AND (:multiplayerOnly = FALSE OR g.multiplayer = TRUE)
            """)
    Page<Game> search(@Param("search") String search, @Param("genreId") Long genreId,
                       @Param("platformId") Long platformId, @Param("multiplayerOnly") boolean multiplayerOnly,
                       Pageable pageable);
}
