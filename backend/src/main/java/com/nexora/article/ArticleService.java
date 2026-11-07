package com.nexora.article;

import com.nexora.article.dto.ArticleDto;
import com.nexora.article.dto.CreateArticleRequest;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleService implements ApplicationRunner {

    private final ArticleRepository articleRepository;
    private final ArticleCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public ArticleService(ArticleRepository articleRepository,
                          ArticleCategoryRepository categoryRepository,
                          UserRepository userRepository) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ArticleDto> getPublishedArticles(Long categoryId, String query) {
        List<Article> list;
        if (query != null && !query.isBlank()) {
            list = articleRepository.searchPublished(query.trim());
        } else if (categoryId != null) {
            list = articleRepository.findByCategoryIdAndPublishedTrueOrderByPublishedAtDesc(categoryId);
        } else {
            list = articleRepository.findByPublishedTrueOrderByPublishedAtDesc();
        }
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ArticleDto getArticleById(Long id) {
        Article a = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + id));
        return toDto(a);
    }

    @Transactional(readOnly = true)
    public List<ArticleDto> getAllArticlesForAdmin() {
        return articleRepository.findAllByOrderByPublishedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ArticleDto createArticle(Long authorId, CreateArticleRequest req) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found: " + authorId));

        ArticleCategory category = null;
        if (req.getCategoryId() != null) {
            category = categoryRepository.findById(req.getCategoryId()).orElse(null);
        } else if (req.getCategoryName() != null && !req.getCategoryName().isBlank()) {
            category = categoryRepository.findByName(req.getCategoryName())
                    .orElseGet(() -> categoryRepository.save(new ArticleCategory(req.getCategoryName())));
        }

        Article article = new Article(req.getTitle(), req.getBody(), category, author, req.isPublished());
        Article saved = articleRepository.save(article);
        return toDto(saved);
    }

    @Transactional
    public ArticleDto updateArticle(Long id, CreateArticleRequest req) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + id));

        article.setTitle(req.getTitle());
        article.setBody(req.getBody());
        article.setPublished(req.isPublished());

        if (req.getCategoryId() != null) {
            article.setCategory(categoryRepository.findById(req.getCategoryId()).orElse(null));
        }

        return toDto(articleRepository.save(article));
    }

    @Transactional
    public void deleteArticle(Long id) {
        articleRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ArticleCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (articleRepository.count() > 0) return;

        User author = userRepository.findByUsername("admin").orElse(null);
        if (author == null) return;

        ArticleCategory tech = categoryRepository.findByName("Technology")
                .orElseGet(() -> categoryRepository.save(new ArticleCategory("Technology")));
        ArticleCategory esports = categoryRepository.findByName("Esports")
                .orElseGet(() -> categoryRepository.save(new ArticleCategory("Esports")));
        ArticleCategory platform = categoryRepository.findByName("Platform Updates")
                .orElseGet(() -> categoryRepository.save(new ArticleCategory("Platform Updates")));

        Article a1 = new Article(
                "Predictive What-If Network Control Goes Live on NEXORA",
                "Today we officially unveiled NEXORA's groundbreaking real-time network adaptation pipeline. "
                + "By forecasting telemetry conditions and executing virtual what-if sandboxing before packet transmission, "
                + "the platform achieves up to 40% reduction in state desynchronization without sacrificing responsiveness.",
                tech, author, true
        );
        Article a2 = new Article(
                "NEXORA Arena Championship 2026 Announced",
                "Get ready for the first annual NEXORA Arena Championship! Teams from across the globe will compete in fast-paced "
                + "top-down tactical shooters. Thanks to authoritative server reconciliation, lag spikes won't decide the championship.",
                esports, author, true
        );
        Article a3 = new Article(
                "NEXORA Tier 1 Multiplayer Expansion: Racers, Football & Tag",
                "We are thrilled to roll out NEXORA's expanded multiplayer catalogue! Experience high-speed drifting in NEXORA Racers, "
                + "precision teamwork in NEXORA Football with critical ball sync, and chaotic evasive action in NEXORA Tag.",
                platform, author, true
        );

        articleRepository.saveAll(List.of(a1, a2, a3));
    }

    private ArticleDto toDto(Article a) {
        String body = a.getBody();
        String summary = body.length() > 150 ? body.substring(0, 150) + "..." : body;
        return new ArticleDto(
                a.getId(),
                a.getTitle(),
                a.getBody(),
                summary,
                a.getCategory() != null ? a.getCategory().getId() : null,
                a.getCategory() != null ? a.getCategory().getName() : "General",
                a.getAuthor().getId(),
                a.getAuthor().getUsername(),
                a.isPublished(),
                a.getPublishedAt()
        );
    }
}
