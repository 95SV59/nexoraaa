package com.nexora.user;

import com.nexora.exception.ResourceNotFoundException;
import com.nexora.library.LibraryRepository;
import com.nexora.multiplayer.room.GamePlayerRepository;
import com.nexora.multiplayer.session.MatchResultRepository;
import com.nexora.review.ReviewRepository;
import com.nexora.user.dto.UpdateProfileRequest;
import com.nexora.user.dto.UserProfileDto;
import com.nexora.wishlist.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final LibraryRepository libraryRepository;
    private final WishlistRepository wishlistRepository;
    private final ReviewRepository reviewRepository;
    private final GamePlayerRepository playerRepository;
    private final MatchResultRepository matchResultRepository;

    public UserService(UserRepository userRepository,
                       LibraryRepository libraryRepository,
                       WishlistRepository wishlistRepository,
                       ReviewRepository reviewRepository,
                       GamePlayerRepository playerRepository,
                       MatchResultRepository matchResultRepository) {
        this.userRepository = userRepository;
        this.libraryRepository = libraryRepository;
        this.wishlistRepository = wishlistRepository;
        this.reviewRepository = reviewRepository;
        this.playerRepository = playerRepository;
        this.matchResultRepository = matchResultRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return buildProfileDto(user);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return buildProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            user.setEmail(req.getEmail().trim());
        }
        if (req.getAvatarUrl() != null && !req.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(req.getAvatarUrl().trim());
        }

        return buildProfileDto(userRepository.save(user));
    }

    private UserProfileDto buildProfileDto(User user) {
        Long uid = user.getId();
        long libCount = libraryRepository.countByUserId(uid);
        long wishCount = wishlistRepository.countByUserId(uid);
        long revCount = reviewRepository.countByUserId(uid);
        long matches = playerRepository.countByUserId(uid);
        long wins = matchResultRepository.countByWinnerUserId(uid);
        double winRate = matches > 0 ? (double) wins / matches * 100.0 : 0.0;

        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().getName() : Role.PLAYER,
                user.getAvatarUrl() != null ? user.getAvatarUrl() : "https://api.dicebear.com/7.x/bottts/svg?seed=" + user.getUsername(),
                user.getStatus().name(),
                user.getCreatedAt(),
                libCount,
                wishCount,
                revCount,
                matches,
                wins,
                Math.round(winRate * 10.0) / 10.0
        );
    }
}
