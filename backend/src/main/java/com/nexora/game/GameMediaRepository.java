package com.nexora.game;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GameMediaRepository extends JpaRepository<GameMedia, Long> {
    List<GameMedia> findByGameId(Long gameId);
    List<GameMedia> findByGameIdAndMediaType(Long gameId, GameMedia.MediaType mediaType);
}
