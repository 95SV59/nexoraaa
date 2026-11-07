package com.nexora.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {
    List<CommunityPost> findByGameSlugOrderByCreatedAtDesc(String gameSlug);
    List<CommunityPost> findByGameSlugAndPostTypeOrderByCreatedAtDesc(String gameSlug, CommunityPostType postType);
}
