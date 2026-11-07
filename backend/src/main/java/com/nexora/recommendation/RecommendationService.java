package com.nexora.recommendation;

import com.nexora.game.Game;
import com.nexora.game.GameMediaRepository;
import com.nexora.game.GameRepository;
import com.nexora.game.dto.GameSummaryDto;
import com.nexora.library.Library;
import com.nexora.library.LibraryRepository;
import com.nexora.wishlist.Wishlist;
import com.nexora.wishlist.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final GameRepository gameRepository;
    private final GameMediaRepository mediaRepository;
    private final LibraryRepository libraryRepository;
    private final WishlistRepository wishlistRepository;

    public RecommendationService(GameRepository gameRepository, GameMediaRepository mediaRepository,
                                 LibraryRepository libraryRepository, WishlistRepository wishlistRepository) {
        this.gameRepository = gameRepository;
        this.mediaRepository = mediaRepository;
        this.libraryRepository = libraryRepository;
        this.wishlistRepository = wishlistRepository;
    }

    @Transactional(readOnly = true)
    public List<RecommendationDto> getRecommendations(Long userId) {
        List<Game> allGames = gameRepository.findAll();
        if (allGames.isEmpty()) {
            return List.of();
        }

        Set<Long> ownedGameIds = new HashSet<>();
        Map<String, Integer> genreFrequency = new HashMap<>();

        if (userId != null) {
            List<Library> lib = libraryRepository.findByUserIdOrderByUpdatedAtDesc(userId);
            for (Library l : lib) {
                ownedGameIds.add(l.getGame().getId());
                if (l.getGame().getGenre() != null) {
                    genreFrequency.merge(l.getGame().getGenre().getName(), 1, Integer::sum);
                }
            }
            List<Wishlist> wish = wishlistRepository.findByUserIdOrderByAddedAtDesc(userId);
            for (Wishlist w : wish) {
                ownedGameIds.add(w.getGame().getId());
                if (w.getGame().getGenre() != null) {
                    genreFrequency.merge(w.getGame().getGenre().getName(), 1, Integer::sum);
                }
            }
        }

        List<RecommendationDto> recommendations = new ArrayList<>();
        Set<Long> recommendedIds = new HashSet<>(ownedGameIds);

        // Rule 1: Favorite Genre Recommendation
        if (!genreFrequency.isEmpty()) {
            String topGenre = genreFrequency.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (topGenre != null) {
                for (Game g : allGames) {
                    if (!recommendedIds.contains(g.getId()) && g.getGenre() != null && topGenre.equalsIgnoreCase(g.getGenre().getName())) {
                        recommendations.add(new RecommendationDto(
                                toSummary(g),
                                "GENRE_AFFINITY",
                                "Because you enjoy " + topGenre + " games"
                        ));
                        recommendedIds.add(g.getId());
                    }
                }
            }
        }

        // Rule 2: Multiplayer Spotlight
        for (Game g : allGames) {
            if (!recommendedIds.contains(g.getId()) && g.isMultiplayer()) {
                recommendations.add(new RecommendationDto(
                        toSummary(g),
                        "MULTIPLAYER_SPOTLIGHT",
                        "Live multiplayer with adaptive network control"
                ));
                recommendedIds.add(g.getId());
            }
        }

        // Rule 3: Platform Trending fallback
        for (Game g : allGames) {
            if (!recommendedIds.contains(g.getId())) {
                recommendations.add(new RecommendationDto(
                        toSummary(g),
                        "TRENDING",
                        "Trending across the NEXORA Community"
                ));
                recommendedIds.add(g.getId());
            }
        }

        return recommendations;
    }

    private GameSummaryDto toSummary(Game g) {
        return GameSummaryDto.from(g, mediaRepository.findByGameId(g.getId()));
    }
}
