package com.nexora.article;

import com.nexora.article.dto.ArticleDto;
import com.nexora.article.dto.CreateArticleRequest;
import com.nexora.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public List<ArticleDto> getArticles(@RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) String search) {
        return articleService.getPublishedArticles(categoryId, search);
    }

    @GetMapping("/{id}")
    public ArticleDto getArticle(@PathVariable Long id) {
        return articleService.getArticleById(id);
    }

    @GetMapping("/categories")
    public List<ArticleCategory> getCategories() {
        return articleService.getAllCategories();
    }

    @PostMapping
    public ResponseEntity<ArticleDto> createArticle(@AuthenticationPrincipal AuthPrincipal principal,
                                                    @Valid @RequestBody CreateArticleRequest request) {
        ArticleDto created = articleService.createArticle(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ArticleDto updateArticle(@PathVariable Long id,
                                    @Valid @RequestBody CreateArticleRequest request) {
        return articleService.updateArticle(id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return Map.of("deleted", true, "id", id);
    }
}
