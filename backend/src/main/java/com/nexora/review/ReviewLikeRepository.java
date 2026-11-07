package com.nexora.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {
    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);
    long countByReviewId(Long reviewId);
    Optional<ReviewLike> findByReviewIdAndUserId(Long reviewId, Long userId);

    @Transactional
    void deleteByReviewIdAndUserId(Long reviewId, Long userId);
}
