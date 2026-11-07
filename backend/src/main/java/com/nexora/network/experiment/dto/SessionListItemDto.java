package com.nexora.network.experiment.dto;

import com.nexora.multiplayer.session.GameSession;
import java.time.Instant;

/** One row of GET /api/analysis/sessions — enough to pick which two sessions to compare. */
public class SessionListItemDto {
    public Long sessionId;
    public Long roomId;
    public String controllerMode;
    public Instant startedAt;
    public Instant endedAt;

    public static SessionListItemDto from(GameSession s) {
        SessionListItemDto dto = new SessionListItemDto();
        dto.sessionId = s.getId();
        dto.roomId = s.getRoomId();
        dto.controllerMode = s.getControllerMode().name();
        dto.startedAt = s.getStartedAt();
        dto.endedAt = s.getEndedAt();
        return dto;
    }
}
