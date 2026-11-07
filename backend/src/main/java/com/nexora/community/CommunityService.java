package com.nexora.community;

import com.nexora.community.dto.CommunityPostDto;
import com.nexora.community.dto.CreateCommunityPostRequest;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommunityService {

    private final CommunityPostRepository postRepository;
    private final UserRepository userRepository;

    public CommunityService(CommunityPostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CommunityPostDto> getPostsForGame(String gameSlug, CommunityPostType postType) {
        List<CommunityPost> posts = (postType != null)
                ? postRepository.findByGameSlugAndPostTypeOrderByCreatedAtDesc(gameSlug.toLowerCase(), postType)
                : postRepository.findByGameSlugOrderByCreatedAtDesc(gameSlug.toLowerCase());

        return posts.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public CommunityPostDto createPost(Long userId, String gameSlug, CreateCommunityPostRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        CommunityPost post = new CommunityPost(
                gameSlug.toLowerCase(),
                user,
                req.title(),
                req.content(),
                req.postType() != null ? req.postType() : CommunityPostType.DISCUSSION
        );

        CommunityPost saved = postRepository.save(post);
        return toDto(saved);
    }

    @Transactional
    public CommunityPostDto likePost(Long postId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
        post.incrementLikes();
        return toDto(postRepository.save(post));
    }

    private CommunityPostDto toDto(CommunityPost post) {
        User author = post.getUser();
        return new CommunityPostDto(
                post.getId(),
                post.getGameSlug(),
                author.getId(),
                author.getUsername(),
                author.getAvatarUrl(),
                post.getTitle(),
                post.getContent(),
                post.getPostType(),
                post.getLikesCount(),
                post.getCreatedAt()
        );
    }
}
