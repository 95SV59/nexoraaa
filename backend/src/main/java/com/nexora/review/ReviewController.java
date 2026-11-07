package com.nexora.review;

import com.nexora.review.dto.CreateReviewRequest;
import com.nexora.review.dto.ReviewDto;
import com.nexora.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping({"/games/{slug}/reviews", "/reviews/{slug}"})
    public List<ReviewDto> getGameReviews(@PathVariable String slug,
                                          @AuthenticationPrincipal AuthPrincipal principal) {
        Long currentUserId = principal != null ? principal.userId() : null;
        return reviewService.getReviewsForGame(slug, currentUserId);
    }

    @PostMapping({"/games/{slug}/reviews", "/reviews/{slug}"})
    public ResponseEntity<ReviewDto> createReview(@PathVariable String slug,
                                                  @AuthenticationPrincipal AuthPrincipal principal,
                                                  @Valid @RequestBody CreateReviewRequest request) {
        ReviewDto created = reviewService.createReview(principal.userId(), slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/reviews/{id}/like")
    public Map<String, Object> toggleLike(@PathVariable Long id,
                                          @AuthenticationPrincipal AuthPrincipal principal) {
        boolean liked = reviewService.toggleLike(principal.userId(), id);
        return Map.of("liked", liked, "reviewId", id);
    }

    @PostMapping("/reviews/{id}/report")
    public Map<String, Object> reportReview(@PathVariable Long id) {
        reviewService.reportReview(id);
        return Map.of("reported", true, "reviewId", id);
    }

    @DeleteMapping("/reviews/{id}")
    public Map<String, Object> deleteReview(@PathVariable Long id,
                                            @AuthenticationPrincipal AuthPrincipal principal) {
        boolean isAdmin = principal != null && "ADMIN".equalsIgnoreCase(principal.role());
        reviewService.deleteReview(principal.userId(), id, isAdmin);
        return Map.of("deleted", true, "reviewId", id);
    }

    @GetMapping("/reviews/my")
    public List<ReviewDto> getMyReviews(@AuthenticationPrincipal AuthPrincipal principal) {
        return reviewService.getReviewsByUser(principal.userId());
    }
}
