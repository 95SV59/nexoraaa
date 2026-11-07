package com.nexora.multiplayer.session;

import com.nexora.multiplayer.session.dto.UserMatchHistoryDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {
    long countByWinnerUserId(Long userId);

    @Query("SELECT new com.nexora.multiplayer.session.dto.UserMatchHistoryDto(" +
            "s.id, r.roomCode, r.gameId, s.startedAt, s.endedAt, mr.durationSec, " +
            "CASE WHEN mr.winnerUserId = :userId THEN true ELSE false END, " +
            "mr.winnerUserId, " +
            "CASE WHEN s.controllerMode = com.nexora.multiplayer.session.GameSession.ControllerMode.PROPOSED THEN 'PROPOSED' ELSE 'BASELINE' END, " +
            "mr.resultSummaryJson) " +
            "FROM MatchResult mr " +
            "JOIN mr.session s " +
            "JOIN com.nexora.multiplayer.room.GameRoom r ON r.id = s.roomId " +
            "WHERE EXISTS (SELECT 1 FROM com.nexora.multiplayer.room.GamePlayer gp WHERE gp.room.id = s.roomId AND gp.userId = :userId) " +
            "ORDER BY s.startedAt DESC")
    List<UserMatchHistoryDto> findMatchHistoryByUserId(@Param("userId") Long userId);
}
