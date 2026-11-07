package com.nexora.review;

import com.nexora.exception.ResourceNotFoundException;
import com.nexora.exception.UnauthorizedException;
import com.nexora.game.Game;
import com.nexora.game.GameRepository;
import com.nexora.library.Library;
import com.nexora.library.LibraryRepository;
import com.nexora.review.dto.CreateReviewRequest;
import com.nexora.review.dto.ReviewDto;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository likeRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final LibraryRepository libraryRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         ReviewLikeRepository likeRepository,
                         GameRepository gameRepository,
                         UserRepository userRepository,
                         LibraryRepository libraryRepository) {
        this.reviewRepository = reviewRepository;
        this.likeRepository = likeRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.libraryRepository = libraryRepository;
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsForGame(String slug, Long currentUserId) {
        return reviewRepository.findByGameSlugOrderByCreatedAtDesc(slug).stream()
                .map(r -> toDto(r, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewDto createReview(Long userId, String slug, CreateReviewRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Game game = gameRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found: " + slug));

        double hoursPlayed = libraryRepository.findByUserIdAndGameSlug(userId, slug)
                .map(Library::getHoursPlayed)
                .orElse(0.0);

        boolean recommended = req.getRecommended() != null ? req.getRecommended() : (req.getRating() >= 3);
        boolean spoiler = Boolean.TRUE.equals(req.getSpoiler());

        Review review = new Review(user, game, req.getRating(), req.getBody(), recommended, hoursPlayed, spoiler);
        Review saved = reviewRepository.save(review);
        return toDto(saved, userId);
    }

    @Transactional
    public boolean toggleLike(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (likeRepository.existsByReviewIdAndUserId(reviewId, userId)) {
            likeRepository.deleteByReviewIdAndUserId(reviewId, userId);
            return false;
        } else {
            likeRepository.save(new ReviewLike(review, user));
            return true;
        }
    }

    @Transactional
    public void reportReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        review.setReported(true);
        reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        if (!isAdmin && !review.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Cannot delete someone else's review");
        }
        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsByUser(Long userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(r -> toDto(r, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getReportedReviews() {
        return reviewRepository.findByReportedTrueOrderByCreatedAtDesc().stream()
                .map(r -> toDto(r, null))
                .collect(Collectors.toList());
    }

    private ReviewDto toDto(Review r, Long currentUserId) {
        long likeCount = likeRepository.countByReviewId(r.getId());
        boolean liked = currentUserId != null && likeRepository.existsByReviewIdAndUserId(r.getId(), currentUserId);
        return new ReviewDto(
                r.getId(),
                r.getGame().getId(),
                r.getGame().getSlug(),
                r.getGame().getTitle(),
                r.getUser().getId(),
                r.getUser().getUsername(),
                r.getUser().getAvatarUrl(),
                r.getRating(),
                r.isRecommended(),
                r.getHoursPlayed(),
                r.isSpoiler(),
                r.getBody(),
                r.isReported(),
                likeCount,
                liked,
                r.getCreatedAt()
        );
    }
}
