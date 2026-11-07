package com.nexora.recommendation;

import com.nexora.security.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public List<RecommendationDto> getRecommendations(@AuthenticationPrincipal AuthPrincipal principal) {
        Long userId = principal != null ? principal.userId() : null;
        return recommendationService.getRecommendations(userId);
    }
}
