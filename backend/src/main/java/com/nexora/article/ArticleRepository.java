package com.nexora.article;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findByPublishedTrueOrderByPublishedAtDesc();
    List<Article> findByCategoryIdAndPublishedTrueOrderByPublishedAtDesc(Long categoryId);
    List<Article> findAllByOrderByPublishedAtDesc();

    @Query("SELECT a FROM Article a WHERE a.published = true AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.body) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY a.publishedAt DESC")
    List<Article> searchPublished(@Param("query") String query);
}
