package com.nexora.article;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ArticleCategoryRepository extends JpaRepository<ArticleCategory, Long> {
    Optional<ArticleCategory> findByName(String name);
}
