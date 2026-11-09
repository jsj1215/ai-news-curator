package com.curator.ainews.notification;

import com.curator.ainews.domain.Article;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageFormatterTest {

    private final MessageFormatter formatter = new MessageFormatter();

    @Test
    void shouldFormatSingleArticle() {
        Article article = buildArticle(1L, "Spring AI 1.0 출시", "요약 내용", 8.5, "HACKER_NEWS");

        String result = formatter.format(List.of(article));

        assertThat(result).contains("Top 1 AI for Backend");
        assertThat(result).contains("1. Spring AI 1.0 출시");
        assertThat(result).contains("요약 내용");
        assertThat(result).contains("8.5");
        assertThat(result).contains("HACKER_NEWS");
        assertThat(result).contains("https://example.com/1");
    }

    @Test
    void shouldFormatMultipleArticles() {
        List<Article> articles = List.of(
            buildArticle(1L, "Article 1", "Summary 1", 9.0, "HN"),
            buildArticle(2L, "Article 2", "Summary 2", 8.0, "REDDIT"),
            buildArticle(3L, "Article 3", "Summary 3", 7.0, "NAVER_D2")
        );

        String result = formatter.format(articles);

        assertThat(result).contains("Top 3 AI for Backend");
        assertThat(result).contains("1. Article 1");
        assertThat(result).contains("2. Article 2");
        assertThat(result).contains("3. Article 3");
    }

    @Test
    void shouldHandleNullSummary() {
        Article article = buildArticle(1L, "Test", null, 7.0, "HN");

        String result = formatter.format(List.of(article));

        assertThat(result).contains("요약 없음");
    }

    @Test
    void shouldHandleNullScore() {
        Article article = buildArticle(1L, "Test", "Summary", null, "HN");

        String result = formatter.format(List.of(article));

        assertThat(result).contains("0.0");
    }

    @Test
    void shouldTruncateLongMessage() {
        StringBuilder longSummary = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longSummary.append("이것은 매우 긴 요약입니다. 반복합니다. ");
        }

        List<Article> articles = List.of();
        for (int i = 0; i < 20; i++) {
            articles = new java.util.ArrayList<>(articles);
            articles.add(buildArticle((long) i, "Article " + i, longSummary.toString(), 8.0, "HN"));
        }

        String result = formatter.format(articles);

        assertThat(result.length()).isLessThanOrEqualTo(4096);
        assertThat(result).endsWith("...");
    }

    @Test
    void shouldTruncateLongSummaryInArticle() {
        String longSummary = "A".repeat(300);

        Article article = buildArticle(1L, "Test", longSummary, 7.0, "HN");

        String result = formatter.format(List.of(article));

        // Summary should be truncated to 200 chars
        assertThat(result).doesNotContain("A".repeat(201));
    }

    private Article buildArticle(Long id, String title, String summary, Double score, String source) {
        return Article.builder()
            .id(id)
            .title(title)
            .url("https://example.com/" + id)
            .source(source)
            .summary(summary)
            .totalScore(score)
            .build();
    }
}
