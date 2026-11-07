package com.nexora.progression;

import com.nexora.social.notification.NotificationService;
import com.nexora.user.Role;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Platform Progression & Gamification Service Unit Tests")
class ProgressionServiceTest {

    private UserProgressionRepository progressionRepository;
    private AchievementRepository achievementRepository;
    private UserAchievementRepository userAchievementRepository;
    private UserRepository userRepository;
    private NotificationService notificationService;

    private ProgressionService progressionService;

    @BeforeEach
    void setUp() {
        progressionRepository = mock(UserProgressionRepository.class);
        achievementRepository = mock(AchievementRepository.class);
        userAchievementRepository = mock(UserAchievementRepository.class);
        userRepository = mock(UserRepository.class);
        notificationService = mock(NotificationService.class);

        progressionService = new ProgressionService(
                progressionRepository,
                achievementRepository,
                userAchievementRepository,
                userRepository,
                notificationService
        );
    }

    @Test
    @DisplayName("Awards XP and advances player level on threshold crossing")
    void testAwardXpIncreasesLevel() {
        Long userId = 10L;
        User user = new User("pilot", "pilot@nexora.gg", "hash", new Role("USER"));
        user.setId(userId);

        UserProgression progression = new UserProgression(user);
        progression.setTotalXp(0);
        progression.setLevel(1);

        when(progressionRepository.findByUserId(userId)).thenReturn(Optional.of(progression));
        when(progressionRepository.save(any(UserProgression.class))).thenReturn(progression);

        // Award 1500 XP (enough to level up past level 1)
        progressionService.awardXp(userId, 1500, "Match Victory");

        assertEquals(1500, progression.getTotalXp());
        assertTrue(progression.getLevel() > 1, "Level must advance after 1500 XP");
        verify(progressionRepository, atLeastOnce()).save(progression);
    }

    @Test
    @DisplayName("Records match outcome and increments matches played and won")
    void testRecordMatchOutcomeWon() {
        Long userId = 20L;
        User user = new User("champion", "champ@nexora.gg", "hash", new Role("USER"));
        user.setId(userId);

        UserProgression progression = new UserProgression(user);
        progression.setMatchesPlayed(5);
        progression.setMatchesWon(3);

        when(progressionRepository.findByUserId(userId)).thenReturn(Optional.of(progression));
        when(progressionRepository.save(any(UserProgression.class))).thenReturn(progression);

        progressionService.recordMatchOutcome(userId, "arena", true);

        assertEquals(6, progression.getMatchesPlayed());
        assertEquals(4, progression.getMatchesWon());
    }

    @Test
    @DisplayName("Calculates correct win rate and progression DTO metrics")
    void testGetProgressionDtoMetrics() {
        Long userId = 30L;
        User user = new User("strategist", "strat@nexora.gg", "hash", new Role("USER"));
        user.setId(userId);

        UserProgression progression = new UserProgression(user);
        progression.setTotalXp(800);
        progression.setLevel(2);
        progression.setMatchesPlayed(10);
        progression.setMatchesWon(7);

        when(progressionRepository.findByUserId(userId)).thenReturn(Optional.of(progression));
        when(userAchievementRepository.countByUserIdAndCompletedTrue(userId)).thenReturn(3L);
        when(achievementRepository.count()).thenReturn(12L);

        var dto = progressionService.getProgressionDto(userId);

        assertNotNull(dto);
        assertEquals(70.0, dto.winRatePct(), 0.1);
        assertEquals(800, dto.totalXp());
        assertEquals(3L, dto.completedAchievementsCount());
        assertEquals(12L, dto.totalAchievementsCount());
    }
}
