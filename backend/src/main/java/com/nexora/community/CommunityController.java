package com.nexora.community;

import com.nexora.community.dto.CommunityPostDto;
import com.nexora.community.dto.CreateCommunityPostRequest;
import com.nexora.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/{gameSlug}")
    public ResponseEntity<List<CommunityPostDto>> getPosts(
            @PathVariable String gameSlug,
            @RequestParam(required = false) CommunityPostType type) {
        return ResponseEntity.ok(communityService.getPostsForGame(gameSlug, type));
    }

    @PostMapping("/{gameSlug}")
    public ResponseEntity<CommunityPostDto> createPost(
            @PathVariable String gameSlug,
            @Valid @RequestBody CreateCommunityPostRequest req,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(communityService.createPost(principal.userId(), gameSlug, req));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<CommunityPostDto> likePost(
            @PathVariable Long postId) {
        return ResponseEntity.ok(communityService.likePost(postId));
    }
}
