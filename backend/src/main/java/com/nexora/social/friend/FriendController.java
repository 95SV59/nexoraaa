package com.nexora.social.friend;

import com.nexora.security.AuthPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    public record SendFriendRequest(String username) {}

    @GetMapping
    public ResponseEntity<List<FriendDto>> getFriends(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(friendService.getFriends(principal.userId()));
    }

    @GetMapping("/requests/incoming")
    public ResponseEntity<List<FriendDto>> getIncomingRequests(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(friendService.getIncomingPendingRequests(principal.userId()));
    }

    @GetMapping("/requests/outgoing")
    public ResponseEntity<List<FriendDto>> getOutgoingRequests(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(friendService.getOutgoingPendingRequests(principal.userId()));
    }

    @PostMapping("/request")
    public ResponseEntity<FriendDto> sendFriendRequest(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody SendFriendRequest req) {
        return ResponseEntity.ok(friendService.sendFriendRequest(principal.userId(), req.username()));
    }

    @PutMapping("/request/{friendshipId}/accept")
    public ResponseEntity<FriendDto> acceptFriendRequest(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long friendshipId) {
        return ResponseEntity.ok(friendService.acceptFriendRequest(principal.userId(), friendshipId));
    }

    @PutMapping("/request/{friendshipId}/decline")
    public ResponseEntity<Void> declineFriendRequest(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long friendshipId) {
        friendService.declineFriendRequest(principal.userId(), friendshipId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{friendUserId}")
    public ResponseEntity<Void> removeFriend(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long friendUserId) {
        friendService.removeFriend(principal.userId(), friendUserId);
        return ResponseEntity.ok().build();
    }
}
