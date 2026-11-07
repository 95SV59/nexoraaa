package com.nexora.wishlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserIdOrderByAddedAtDesc(Long userId);
    boolean existsByUserIdAndGameId(Long userId, Long gameId);
    boolean existsByUserIdAndGameSlug(Long userId, String slug);
    Optional<Wishlist> findByUserIdAndGameId(Long userId, Long gameId);
    Optional<Wishlist> findByUserIdAndGameSlug(Long userId, String slug);
    long countByUserId(Long userId);

    @Transactional
    void deleteByUserIdAndGameId(Long userId, Long gameId);

    @Transactional
    void deleteByUserIdAndGameSlug(Long userId, String slug);
}
