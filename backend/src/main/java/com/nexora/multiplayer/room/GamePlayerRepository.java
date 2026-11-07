package com.nexora.multiplayer.room;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {
    List<GamePlayer> findByRoomIdAndLeftAtIsNull(Long roomId);
    Optional<GamePlayer> findByRoomIdAndUserIdAndLeftAtIsNull(Long roomId, Long userId);
    long countByUserId(Long userId);
}
