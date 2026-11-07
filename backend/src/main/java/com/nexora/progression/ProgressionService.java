package com.nexora.progression;

import com.nexora.progression.dto.AchievementDto;
import com.nexora.progression.dto.UserProgressionDto;
import com.nexora.social.notification.NotificationService;
import com.nexora.social.notification.NotificationType;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProgressionService {

    private final UserProgressionRepository progressionRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ProgressionService(UserProgressionRepository progressionRepository,
                              AchievementRepository achievementRepository,
                              UserAchievementRepository userAchievementRepository,
                              UserRepository userRepository,
                              NotificationService notificationService) {
        this.progressionRepository = progressionRepository;
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public UserProgression getOrCreateProgression(Long userId) {
        return progressionRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            return progressionRepository.save(new UserProgression(user));
        });
    }

    @Transactional(readOnly = true)
    public UserProgressionDto getProgressionDto(Long userId) {
        UserProgression p = getOrCreateProgression(userId);
        User user = p.getUser();

        int currentLvl = p.getLevel();
        int currentFloor = getXpForLevel(currentLvl);
        int nextThreshold = getXpForLevel(currentLvl + 1);
        int xpSpan = Math.max(1, nextThreshold - currentFloor);
        int xpInLevel = Math.max(0, p.getTotalXp() - currentFloor);
        double progressPct = Math.min(100.0, Math.round((100.0 * xpInLevel / xpSpan) * 10.0) / 10.0);

        double winRate = p.getMatchesPlayed() == 0 ? 0.0
                : Math.round((100.0 * p.getMatchesWon() / p.getMatchesPlayed()) * 10.0) / 10.0;

        long completedCount = userAchievementRepository.countByUserIdAndCompletedTrue(userId);
        long totalAchievements = achievementRepository.count();

        return new UserProgressionDto(
                user.getId(),
                user.getUsername(),
                p.getTotalXp(),
                p.getLevel(),
                p.getRankTitle(),
                p.getSeasonPassTier(),
                p.getMatchesPlayed(),
                p.getMatchesWon(),
                winRate,
                currentFloor,
                nextThreshold,
                progressPct,
                completedCount,
                totalAchievements
        );
    }

    @Transactional
    public void awardXp(Long userId, int xpAmount, String reason) {
        if (xpAmount <= 0) return;
        UserProgression p = getOrCreateProgression(userId);
        int oldLevel = p.getLevel();
        p.setTotalXp(p.getTotalXp() + xpAmount);

        int newLevel = calculateLevel(p.getTotalXp());
        if (newLevel != oldLevel) {
            p.setLevel(newLevel);
            p.setRankTitle(getRankTitleForLevel(newLevel));
            notificationService.sendNotification(
                    userId,
                    NotificationType.SYSTEM,
                    "Level Up!",
                    "Congratulations! You reached Level " + newLevel + " (" + p.getRankTitle() + ").",
                    "/profile"
            );
            if (newLevel >= 10) {
                checkAndUnlockAchievement(userId, "REACH_LEVEL_10", 10);
            }
        }
        progressionRepository.save(p);
    }

    @Transactional
    public void recordMatchOutcome(Long userId, String gameSlug, boolean won) {
        UserProgression p = getOrCreateProgression(userId);
        p.setMatchesPlayed(p.getMatchesPlayed() + 1);
        if (won) {
            p.setMatchesWon(p.getMatchesWon() + 1);
        }
        progressionRepository.save(p);

        // Base participation XP + bonus for winning
        int xpEarned = won ? 150 : 75;
        awardXp(userId, xpEarned, "Match Completion (" + gameSlug + ")");

        // Evaluate milestone achievements
        checkAndUnlockAchievement(userId, "FIRST_MATCH", 1);
        if (won) {
            if ("arena".equalsIgnoreCase(gameSlug)) checkAndUnlockAchievement(userId, "WIN_ARENA", 1);
            if ("racers".equalsIgnoreCase(gameSlug)) checkAndUnlockAchievement(userId, "WIN_RACERS", 1);
            if ("football".equalsIgnoreCase(gameSlug)) checkAndUnlockAchievement(userId, "SCORE_GOAL", 1);
            if ("tag".equalsIgnoreCase(gameSlug)) checkAndUnlockAchievement(userId, "EVADE_TAG", 1);
        }
    }

    @Transactional
    public void checkAndUnlockAchievement(Long userId, String criteriaType, int increment) {
        List<Achievement> matching = achievementRepository.findByCriteriaType(criteriaType);
        if (matching.isEmpty()) return;

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        for (Achievement ach : matching) {
            UserAchievement ua = userAchievementRepository.findByUserIdAndAchievementId(userId, ach.getId())
                    .orElseGet(() -> new UserAchievement(user, ach));

            if (ua.isCompleted()) continue;

            ua.setProgress(ua.getProgress() + increment);
            if (ua.getProgress() >= ach.getCriteriaThreshold()) {
                ua.markUnlocked();
                userAchievementRepository.save(ua);

                // Award XP reward
                awardXp(userId, ach.getXpReward(), "Achievement Unlocked: " + ach.getTitle());

                // Notify player
                notificationService.sendNotification(
                        userId,
                        NotificationType.SYSTEM,
                        "Achievement Unlocked!",
                        "Unlocked: " + ach.getTitle() + " (" + ach.getDescription() + ") +" + ach.getXpReward() + " XP",
                        "/achievements"
                );
            } else {
                userAchievementRepository.save(ua);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<AchievementDto> getAchievementsForUser(Long userId, String gameSlug) {
        List<Achievement> all = (gameSlug == null || gameSlug.isBlank())
                ? achievementRepository.findAll()
                : achievementRepository.findByGameSlug(gameSlug.toLowerCase());

        Map<Long, UserAchievement> userMap = userAchievementRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(ua -> ua.getAchievement().getId(), ua -> ua, (a, b) -> a));

        return all.stream().map(a -> {
            UserAchievement ua = userMap.get(a.getId());
            boolean completed = ua != null && ua.isCompleted();
            int progress = ua != null ? ua.getProgress() : 0;
            return new AchievementDto(
                    a.getId(),
                    a.getGameSlug(),
                    a.getSlug(),
                    a.getTitle(),
                    a.getDescription(),
                    a.getBadgeIcon(),
                    a.getIconUrl(),
                    a.getXpReward(),
                    a.getCriteriaType(),
                    a.getCriteriaThreshold(),
                    a.getRarityTier(),
                    completed,
                    progress,
                    ua != null ? ua.getUnlockedAt() : null
            );
        }).sorted(Comparator.comparing(AchievementDto::completed).reversed()
                .thenComparing(AchievementDto::id))
          .collect(Collectors.toList());
    }

    public static int getXpForLevel(int level) {
        if (level <= 1) return 0;
        return (int) Math.round(250 * Math.pow(level - 1, 1.6));
    }

    public static int calculateLevel(int totalXp) {
        if (totalXp <= 0) return 1;
        int level = 1;
        while (getXpForLevel(level + 1) <= totalXp) {
            level++;
        }
        return level;
    }

    public static String getRankTitleForLevel(int level) {
        if (level < 3) return "Rookie Operative";
        if (level < 5) return "Field Specialist";
        if (level < 8) return "Tactical Operative";
        if (level < 12) return "Cyber Vanguard";
        if (level < 20) return "Elite Phantom";
        return "Apex Grandmaster";
    }
}
