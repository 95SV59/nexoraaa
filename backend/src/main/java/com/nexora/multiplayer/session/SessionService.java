package com.nexora.multiplayer.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.multiplayer.engine.MatchResultData;
import com.nexora.network.persistence.SessionBroadcastStatsEntity;
import com.nexora.network.persistence.SessionBroadcastStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges the in-memory game loop (GameLoopManager / MultiplayerGame) to persistence.
 * Only called at match start and match end — never per-tick — so the high-frequency
 * loop is not slowed down by database writes.
 */
@Service
public class SessionService {

    private final GameSessionRepository sessionRepository;
    private final MatchResultRepository matchResultRepository;
    private final SessionBroadcastStatsRepository broadcastStatsRepository;
    private final ObjectMapper objectMapper;

    public SessionService(GameSessionRepository sessionRepository,
                           MatchResultRepository matchResultRepository,
                           SessionBroadcastStatsRepository broadcastStatsRepository,
                           ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.matchResultRepository = matchResultRepository;
        this.broadcastStatsRepository = broadcastStatsRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GameSession startSession(Long roomId, GameSession.ControllerMode controllerMode) {
        GameSession session = new GameSession(roomId);
        session.setControllerMode(controllerMode);
        return sessionRepository.save(session);
    }

    @Transactional
    public void endSession(GameSession session, MatchResultData result, Long winnerUserId) {
        session.markEnded();
        sessionRepository.save(session);

        String json;
        try {
            json = objectMapper.writeValueAsString(result.getSummary());
        } catch (Exception e) {
            json = "{}"; // never let a serialization hiccup block ending the match
        }
        matchResultRepository.save(new MatchResult(session, winnerUserId, result.getDurationSec(), json));
    }

    @Transactional
    public void recordBroadcastStats(Long sessionId, long totalTicks, long totalBroadcasts) {
        broadcastStatsRepository.save(new SessionBroadcastStatsEntity(sessionId, totalTicks, totalBroadcasts));
    }
}
