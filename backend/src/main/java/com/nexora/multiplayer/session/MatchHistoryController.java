package com.nexora.multiplayer.session;

import com.nexora.multiplayer.session.dto.UserMatchHistoryDto;
import com.nexora.security.AuthPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoint for a user's match history.
 * GET /api/matches/history  — returns last N matches the authenticated user participated in.
 */
@RestController
@RequestMapping("/api/matches")
public class MatchHistoryController {

    private final MatchResultRepository matchResultRepository;

    public MatchHistoryController(MatchResultRepository matchResultRepository) {
        this.matchResultRepository = matchResultRepository;
    }

    @GetMapping("/history")
    public ResponseEntity<List<UserMatchHistoryDto>> getHistory(
            @AuthenticationPrincipal AuthPrincipal principal) {
        List<UserMatchHistoryDto> history =
                matchResultRepository.findMatchHistoryByUserId(principal.userId());
        return ResponseEntity.ok(history);
    }
}
