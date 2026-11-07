package com.nexora.wishlist;

import com.nexora.exception.ResourceNotFoundException;
import com.nexora.game.Game;
import com.nexora.game.GameMedia;
import com.nexora.game.GameMediaRepository;
import com.nexora.game.GameRepository;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import com.nexora.wishlist.dto.WishlistItemDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final GameMediaRepository mediaRepository;

    public WishlistService(WishlistRepository wishlistRepository, GameRepository gameRepository,
                           UserRepository userRepository, GameMediaRepository mediaRepository) {
        this.wishlistRepository = wishlistRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.mediaRepository = mediaRepository;
    }

    @Transactional(readOnly = true)
    public List<WishlistItemDto> getUserWishlist(Long userId) {
        return wishlistRepository.findByUserIdOrderByAddedAtDesc(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public WishlistItemDto addToWishlist(Long userId, String slug) {
        if (wishlistRepository.existsByUserIdAndGameSlug(userId, slug)) {
            Wishlist existing = wishlistRepository.findByUserIdAndGameSlug(userId, slug)
                    .orElseThrow();
            return toDto(existing);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Game game = gameRepository.findBySlug(slug)
                .or(() -> {
                    try {
                        return gameRepository.findById(Long.parseLong(slug));
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                })
                .orElseThrow(() -> new ResourceNotFoundException("Game not found: " + slug));

        Wishlist wishlist = new Wishlist(user, game);
        Wishlist saved = wishlistRepository.save(wishlist);
        return toDto(saved);
    }

    @Transactional
    public void removeFromWishlist(Long userId, String slug) {
        wishlistRepository.deleteByUserIdAndGameSlug(userId, slug);
    }

    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, String slug) {
        return wishlistRepository.existsByUserIdAndGameSlug(userId, slug);
    }

    private WishlistItemDto toDto(Wishlist w) {
        Game g = w.getGame();
        String thumbnail = mediaRepository.findByGameIdAndMediaType(g.getId(), GameMedia.MediaType.SCREENSHOT)
                .stream().findFirst().map(GameMedia::getUrl)
                .orElse("https://placehold.co/640x360?text=" + g.getSlug());
        String genreName = g.getGenre() != null ? g.getGenre().getName() : "Uncategorized";

        return new WishlistItemDto(
                w.getId(),
                g.getId(),
                g.getSlug(),
                g.getTitle(),
                g.getDescription(),
                genreName,
                thumbnail,
                g.isMultiplayer(),
                w.getAddedAt()
        );
    }
}
