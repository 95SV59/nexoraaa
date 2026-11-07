package com.nexora.library;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface LibraryRepository extends JpaRepository<Library, Long> {
    List<Library> findByUserIdOrderByUpdatedAtDesc(Long userId);
    List<Library> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, LibraryStatus status);
    List<Library> findByUserIdAndIsFavoriteTrueOrderByUpdatedAtDesc(Long userId);
    Optional<Library> findByUserIdAndGameSlug(Long userId, String slug);
    Optional<Library> findByUserIdAndGameId(Long userId, Long gameId);
    boolean existsByUserIdAndGameSlug(Long userId, String slug);
    long countByUserId(Long userId);
    long countByUserIdAndStatus(Long userId, LibraryStatus status);
    long countByUserIdAndIsFavoriteTrue(Long userId);

    @Transactional
    void deleteByUserIdAndGameSlug(Long userId, String slug);
}
