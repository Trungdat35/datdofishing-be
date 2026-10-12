package com.datdofishing.content.repo;

import com.datdofishing.content.domain.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Page<Article> findByStatus(String status, Pageable pageable);

    Optional<Article> findBySlugAndStatus(String slug, String status);

    boolean existsBySlug(String slug);

    /** Lọc theo thẻ trong cột JSON bằng JSON_CONTAINS của MySQL. */
    @Query(value = "SELECT * FROM articles WHERE status = 'PUBLISHED' AND JSON_CONTAINS(tags, CAST(:tagJson AS JSON))",
            countQuery = "SELECT count(*) FROM articles WHERE status = 'PUBLISHED' AND JSON_CONTAINS(tags, CAST(:tagJson AS JSON))",
            nativeQuery = true)
    Page<Article> findPublishedByTag(String tagJson, Pageable pageable);

    @Modifying
    @Query("update Article a set a.viewCount = a.viewCount + 1 where a.id = :id")
    void incrementView(Long id);
}
