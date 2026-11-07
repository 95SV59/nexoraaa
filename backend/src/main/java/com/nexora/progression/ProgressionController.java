package com.nexora.progression;

import com.nexora.progression.dto.AchievementDto;
import com.nexora.progression.dto.LeaderboardEntryDto;
import com.nexora.progression.dto.UserProgressionDto;
import com.nexora.security.AuthPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProgressionController {

    private final ProgressionService progressionService;
    private final LeaderboardService leaderboardService;

    public ProgressionController(ProgressionService progressionService,
                                 LeaderboardService leaderboardService) {
        this.progressionService = progressionService;
        this.leaderboardService = leaderboardService;
    }

    @GetMapping({"/progression/me", "/progression/profile"})
    public ResponseEntity<UserProgressionDto> getMyProgression(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(progressionService.getProgressionDto(principal.userId()));
    }

    public record MatchOutcomeRequest(String gameSlug, boolean won, Integer score) {}

    @PostMapping("/progression/match-completed")
    public ResponseEntity<UserProgressionDto> recordMatchCompleted(
            @RequestBody MatchOutcomeRequest req,
            @AuthenticationPrincipal AuthPrincipal principal) {
        progressionService.recordMatchOutcome(principal.userId(),
                req.gameSlug() != null ? req.gameSlug() : "arena", req.won());
        return ResponseEntity.ok(progressionService.getProgressionDto(principal.userId()));
    }

    @GetMapping({"/achievements", "/progression/achievements"})
    public ResponseEntity<List<AchievementDto>> getAchievements(
            @RequestParam(required = false) String game,
            @AuthenticationPrincipal AuthPrincipal principal) {
        Long userId = principal != null ? principal.userId() : null;
        if (userId != null) {
            return ResponseEntity.ok(progressionService.getAchievementsForUser(userId, game));
        } else {
            // Unauthenticated view (no user progress)
            return ResponseEntity.ok(progressionService.getAchievementsForUser(1L, game).stream()
                    .map(a -> new AchievementDto(a.id(), a.gameSlug(), a.slug(), a.title(), a.description(),
                            a.badgeIcon(), a.iconUrl(), a.xpReward(), a.criteriaType(), a.criteriaThreshold(),
                            a.rarityTier(), false, 0, null))
                    .toList());
        }
    }

    @GetMapping("/achievements/me")
    public ResponseEntity<List<AchievementDto>> getMyAchievements(
            @RequestParam(required = false) String game,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(progressionService.getAchievementsForUser(principal.userId(), game));
    }

    @GetMapping({"/leaderboards", "/leaderboard"})
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(
            @RequestParam(defaultValue = "arena") String game,
            @RequestParam(defaultValue = "1") int season,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(leaderboardService.getGlobalLeaderboard(game, season, limit));
    }

    @GetMapping({"/leaderboards/{game}", "/leaderboard/{game}"})
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboardForGame(
            @PathVariable String game,
            @RequestParam(defaultValue = "1") int season,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(leaderboardService.getGlobalLeaderboard(game, season, limit));
    }

    @GetMapping("/leaderboards/friends")
    public ResponseEntity<List<LeaderboardEntryDto>> getFriendsLeaderboard(
            @RequestParam(defaultValue = "arena") String game,
            @RequestParam(defaultValue = "1") int season,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(leaderboardService.getFriendsLeaderboard(principal.userId(), game, season));
    }
}
