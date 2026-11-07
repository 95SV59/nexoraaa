package com.nexora.social.presence;

import com.nexora.security.AuthPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    public record UpdatePresenceRequest(PresenceStatus status, String currentGameSlug, String currentRoomCode) {}

    @GetMapping({"", "/me"})
    public ResponseEntity<PresenceDto> getMyPresence(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(presenceService.getPresence(principal.userId()));
    }

    @PutMapping
    public ResponseEntity<PresenceDto> updatePresence(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody UpdatePresenceRequest req) {
        PresenceDto dto = presenceService.updatePresence(
                principal.userId(),
                req.status() != null ? req.status() : PresenceStatus.ONLINE,
                req.currentGameSlug(),
                req.currentRoomCode()
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<PresenceDto> getPresence(@PathVariable Long userId) {
        return ResponseEntity.ok(presenceService.getPresence(userId));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<PresenceDto>> getBatchPresence(@RequestBody List<Long> userIds) {
        return ResponseEntity.ok(presenceService.getPresences(userIds));
    }
}
