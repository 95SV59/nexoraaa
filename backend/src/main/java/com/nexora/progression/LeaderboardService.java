package com.nexora.progression;

import com.nexora.progression.dto.LeaderboardEntryDto;
import com.nexora.social.friend.Friendship;
import com.nexora.social.friend.FriendshipRepository;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public LeaderboardService(LeaderboardRepository leaderboardRepository,
                              FriendshipRepository friendshipRepository,
                              UserRepository userRepository) {
        this.leaderboardRepository = leaderboardRepository;
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> getGlobalLeaderboard(String gameSlug, int season, int limit) {
        String slug = (gameSlug == null || gameSlug.isBlank()) ? "arena" : gameSlug.toLowerCase();
        int safeLimit = Math.max(1, Math.min(limit, 100));

        List<LeaderboardEntry> entries = leaderboardRepository.findByGameSlugAndSeasonOrderByScoreDesc(
                slug, season, PageRequest.of(0, safeLimit));

        return toDtoList(entries);
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> getFriendsLeaderboard(Long currentUserId, String gameSlug, int season) {
        String slug = (gameSlug == null || gameSlug.isBlank()) ? "arena" : gameSlug.toLowerCase();

        Set<Long> userIds = new HashSet<>();
        userIds.add(currentUserId);

        List<Friendship> friendships = friendshipRepository.findAllAcceptedFriends(currentUserId);
        for (Friendship f : friendships) {
            if (f.getUser().getId().equals(currentUserId)) {
                userIds.add(f.getFriend().getId());
            } else {
                userIds.add(f.getUser().getId());
            }
        }

        List<LeaderboardEntry> entries = leaderboardRepository.findByGameSlugAndSeasonAndUserIdInOrderByScoreDesc(
                slug, season, new ArrayList<>(userIds));

        return toDtoList(entries);
    }

    @Transactional
    public void recordMatchScore(Long userId, String gameSlug, boolean won, int customDelta) {
        String slug = (gameSlug == null || gameSlug.isBlank()) ? "arena" : gameSlug.toLowerCase();
        int season = 1;

        LeaderboardEntry entry = leaderboardRepository.findByGameSlugAndUserIdAndSeason(slug, userId, season)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                    return new LeaderboardEntry(slug, user, season);
                });

        entry.setMatchesPlayed(entry.getMatchesPlayed() + 1);
        if (won) {
            entry.setWins(entry.getWins() + 1);
        }

        int scoreDelta = customDelta != 0 ? customDelta : (won ? 50 : 10);
        entry.setScore(Math.max(0, entry.getScore() + scoreDelta));
        entry.setRankTier(computeRankTier(entry.getScore()));

        leaderboardRepository.save(entry);
    }

    private List<LeaderboardEntryDto> toDtoList(List<LeaderboardEntry> entries) {
        List<LeaderboardEntryDto> list = new ArrayList<>();
        int rank = 1;
        for (LeaderboardEntry e : entries) {
            User u = e.getUser();
            double winRate = e.getMatchesPlayed() == 0 ? 0.0
                    : Math.round((100.0 * e.getWins() / e.getMatchesPlayed()) * 10.0) / 10.0;

            list.add(new LeaderboardEntryDto(
                    rank++,
                    u.getId(),
                    u.getUsername(),
                    u.getAvatarUrl(),
                    e.getGameSlug(),
                    e.getScore(),
                    e.getRankTier(),
                    e.getMatchesPlayed(),
                    e.getWins(),
                    winRate,
                    e.getSeason()
            ));
        }
        return list;
    }

    public static String computeRankTier(int score) {
        if (score >= 2800) return "MASTER";
        if (score >= 2400) return "DIAMOND";
        if (score >= 2000) return "PLATINUM";
        if (score >= 1600) return "GOLD";
        if (score >= 1200) return "SILVER";
        return "BRONZE";
    }
}
