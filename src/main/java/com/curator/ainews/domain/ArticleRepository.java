package com.curator.ainews.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    boolean existsByUrl(String url);

    List<Article> findByCollectedAtAfterAndSentFalseOrderByTotalScoreDesc(LocalDateTime after);

    List<Article> findByTotalScoreIsNullAndCollectedAtAfter(LocalDateTime after);

    @Query("SELECT a FROM Article a WHERE a.sent = true AND a.sentAt >= :start AND a.sentAt < :end ORDER BY a.sentAt DESC, a.totalScore DESC")
    List<Article> findSentArticlesByDateRange(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("SELECT DISTINCT CAST(a.sentAt AS date) FROM Article a WHERE a.sent = true ORDER BY CAST(a.sentAt AS date) DESC")
    List<java.sql.Date> findDistinctSentDates();

    long countByCreatedAtAfter(LocalDateTime after);

    long countBySentTrue();

    long countBySource(String source);

    @Query("SELECT a.source, COUNT(a) FROM Article a GROUP BY a.source ORDER BY COUNT(a) DESC")
    List<Object[]> countBySourceGrouped();

    @Query("SELECT AVG(a.totalScore) FROM Article a WHERE a.totalScore IS NOT NULL")
    Double findAverageTotalScore();

    @Query("SELECT a.source, AVG(a.totalScore) FROM Article a WHERE a.totalScore IS NOT NULL GROUP BY a.source")
    List<Object[]> findAverageScoreBySource();

    List<Article> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT a FROM Article a WHERE a.sent = true ORDER BY a.sentAt DESC, a.totalScore DESC")
    List<Article> findAllSentArticlesOrderBySentAtDesc();
}
