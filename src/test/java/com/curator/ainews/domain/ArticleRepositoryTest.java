package com.curator.ainews.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ArticleRepositoryTest {

    @Autowired
    private ArticleRepository articleRepository;

    @Test
    void shouldSaveAndFindArticle() {
        Article article = Article.builder()
            .title("Spring AI 1.0 Released")
            .url("https://spring.io/blog/spring-ai-1.0")
            .source("HACKER_NEWS")
            .content("Spring AI is now GA...")
            .publishedAt(LocalDateTime.now())
            .build();

        Article saved = articleRepository.save(article);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(articleRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void shouldCheckDuplicateByUrl() {
        Article article = Article.builder()
            .title("Test Article")
            .url("https://example.com/unique-url")
            .source("REDDIT")
            .build();

        articleRepository.save(article);

        assertThat(articleRepository.existsByUrl("https://example.com/unique-url")).isTrue();
        assertThat(articleRepository.existsByUrl("https://example.com/other-url")).isFalse();
    }

    @Test
    void shouldFindUnevaluatedArticles() {
        Article evaluated = Article.builder()
            .title("Evaluated")
            .url("https://example.com/1")
            .source("HN")
            .totalScore(8.5)
            .build();

        Article unevaluated = Article.builder()
            .title("Unevaluated")
            .url("https://example.com/2")
            .source("HN")
            .build();

        articleRepository.save(evaluated);
        articleRepository.save(unevaluated);

        var results = articleRepository.findByTotalScoreIsNullAndCollectedAtAfter(
            LocalDateTime.now().minusHours(1)
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Unevaluated");
    }

    @Test
    void shouldFindUnsentArticlesOrderedByScore() {
        articleRepository.save(Article.builder()
            .title("Low Score").url("https://example.com/low")
            .source("HN").totalScore(5.0).sent(false).build());

        articleRepository.save(Article.builder()
            .title("High Score").url("https://example.com/high")
            .source("HN").totalScore(9.0).sent(false).build());

        articleRepository.save(Article.builder()
            .title("Already Sent").url("https://example.com/sent")
            .source("HN").totalScore(10.0).sent(true)
            .sentAt(LocalDateTime.now()).build());

        var results = articleRepository.findByCollectedAtAfterAndSentFalseOrderByTotalScoreDesc(
            LocalDateTime.now().minusHours(1)
        );

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTitle()).isEqualTo("High Score");
    }
}
