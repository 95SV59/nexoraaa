package com.nexora.review;

import com.nexora.exception.UnauthorizedException;
import com.nexora.game.Game;
import com.nexora.game.GameRepository;
import com.nexora.library.Library;
import com.nexora.library.LibraryRepository;
import com.nexora.library.LibraryStatus;
import com.nexora.review.dto.CreateReviewRequest;
import com.nexora.review.dto.ReviewDto;
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

class ReviewServiceTest {

    private ReviewRepository reviewRepository;
    private ReviewLikeRepository likeRepository;
    private GameRepository gameRepository;
    private UserRepository userRepository;
    private LibraryRepository libraryRepository;
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewRepository = mock(ReviewRepository.class);
        likeRepository = mock(ReviewLikeRepository.class);
        gameRepository = mock(GameRepository.class);
        userRepository = mock(UserRepository.class);
        libraryRepository = mock(LibraryRepository.class);

        reviewService = new ReviewService(reviewRepository, likeRepository, gameRepository, userRepository, libraryRepository);
    }

    @Test
    @DisplayName("Creates new review incorporating player library playtime")
    void testCreateReviewWithPlaytime() {
        Long userId = 1L;
        String slug = "cyber-arena";

        User user = new User("reviewer", "r@nexora.gg", "hash", new Role("USER"));
        user.setId(userId);
        Game game = new Game();
        game.setId(10L);
        game.setSlug(slug);
        game.setTitle("Cyber Arena");

        Library library = new Library(user, game, LibraryStatus.COMPLETED);
        library.setHoursPlayed(14.5);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(gameRepository.findBySlug(slug)).thenReturn(Optional.of(game));
        when(libraryRepository.findByUserIdAndGameSlug(userId, slug)).thenReturn(Optional.of(library));

        CreateReviewRequest req = new CreateReviewRequest(5, "Amazing game, smooth netcode!", true, false);

        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setId(50L);
            return r;
        });

        ReviewDto dto = reviewService.createReview(userId, slug, req);

        assertNotNull(dto);
        assertEquals(5, dto.getRating());
        assertEquals("Amazing game, smooth netcode!", dto.getBody());
        assertEquals(14.5, dto.getHoursPlayed());
        assertTrue(dto.isRecommended());
        assertFalse(dto.isSpoiler());
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    @DisplayName("Toggles like status on a review")
    void testToggleLike() {
        Long userId = 1L;
        Long reviewId = 50L;

        User user = new User("liker", "l@nexora.gg", "hash", new Role("USER"));
        user.setId(userId);
        Game game = new Game();
        game.setId(10L);
        game.setSlug("cyber-arena");
        Review review = new Review(user, game, 4, "Great", true, 5.0, false);
        review.setId(reviewId);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // First click: Not liked yet -> saves like
        when(likeRepository.existsByReviewIdAndUserId(reviewId, userId)).thenReturn(false);
        boolean likedFirst = reviewService.toggleLike(userId, reviewId);
        assertTrue(likedFirst);
        verify(likeRepository, times(1)).save(any(ReviewLike.class));

        // Second click: Already liked -> deletes like
        when(likeRepository.existsByReviewIdAndUserId(reviewId, userId)).thenReturn(true);
        boolean likedSecond = reviewService.toggleLike(userId, reviewId);
        assertFalse(likedSecond);
        verify(likeRepository, times(1)).deleteByReviewIdAndUserId(reviewId, userId);
    }

    @Test
    @DisplayName("Reports inappropriate review")
    void testReportReview() {
        Long reviewId = 99L;
        User user = new User("author", "a@nexora.gg", "hash", new Role("USER"));
        user.setId(2L);
        Game game = new Game();
        game.setId(10L);
        Review review = new Review(user, game, 1, "Offensive content", false, 0.5, false);
        review.setId(reviewId);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        reviewService.reportReview(reviewId);

        assertTrue(review.isReported());
        verify(reviewRepository, times(1)).save(review);
    }

    @Test
    @DisplayName("Prevents unauthorized player from deleting another user's review")
    void testUnauthorizedDeleteReview() {
        Long authorId = 1L;
        Long attackerId = 2L;
        Long reviewId = 100L;

        User author = new User("author", "a@nexora.gg", "hash", new Role("USER"));
        author.setId(authorId);
        Game game = new Game();
        game.setId(10L);
        Review review = new Review(author, game, 3, "Decent", true, 2.0, false);
        review.setId(reviewId);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        assertThrows(UnauthorizedException.class, () ->
                reviewService.deleteReview(attackerId, reviewId, false)
        );
        verify(reviewRepository, never()).delete(any());
    }
}
