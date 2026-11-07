package com.nexora.multiplayer.party;

import com.nexora.security.AuthPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/parties")
public class PartyController {

    private final PartyService partyService;

    public PartyController(PartyService partyService) {
        this.partyService = partyService;
    }

    public record InviteRequest(Long friendUserId) {}
    public record LaunchMatchRequest(String gameSlug) {}

    @GetMapping("/current")
    public ResponseEntity<PartyDto> getCurrentParty(@AuthenticationPrincipal AuthPrincipal principal) {
        return partyService.getCurrentParty(principal.userId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<PartyDto> createParty(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(partyService.createParty(principal.userId()));
    }

    @PostMapping("/invite")
    public ResponseEntity<Void> inviteToParty(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody InviteRequest req) {
        partyService.inviteToParty(principal.userId(), req.friendUserId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/invitations/{invitationId}/respond")
    public ResponseEntity<PartyDto> respondInvitation(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long invitationId,
            @RequestParam boolean accept) {
        PartyDto party = partyService.respondInvitation(invitationId, principal.userId(), accept);
        return ResponseEntity.ok(party);
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leaveParty(@AuthenticationPrincipal AuthPrincipal principal) {
        partyService.leaveParty(principal.userId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/launch")
    public ResponseEntity<Map<String, String>> launchMatch(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody LaunchMatchRequest req) {
        String roomCode = partyService.launchPartyMatch(principal.userId(), req.gameSlug());
        return ResponseEntity.ok(Map.of("roomCode", roomCode));
    }
}
