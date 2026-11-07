package com.nexora.multiplayer.matchmaking;

import com.nexora.multiplayer.matchmaking.dto.MatchmakingRequest;
import com.nexora.multiplayer.matchmaking.dto.MatchmakingStatusResponse;
import com.nexora.security.AuthPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matchmaking")
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    public MatchmakingController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @PostMapping("/join")
    public ResponseEntity<MatchmakingStatusResponse> joinQueue(
            @RequestBody(required = false) MatchmakingRequest req,
            @AuthenticationPrincipal AuthPrincipal principal) {
        String slug = (req != null && req.gameSlug() != null) ? req.gameSlug() : "arena";
        return ResponseEntity.ok(matchmakingService.enqueue(principal.userId(), principal.username(), slug));
    }

    @PostMapping("/leave")
    public ResponseEntity<MatchmakingStatusResponse> leaveQueue(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(matchmakingService.dequeue(principal.userId()));
    }

    @GetMapping("/status")
    public ResponseEntity<MatchmakingStatusResponse> getStatus(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(matchmakingService.getStatus(principal.userId()));
    }
}
