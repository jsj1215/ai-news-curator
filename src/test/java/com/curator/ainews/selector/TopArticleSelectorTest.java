package com.curator.ainews.selector;

import com.curator.ainews.domain.Article;
import com.curator.ainews.domain.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TopArticleSelectorTest {

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private TopArticleSelector selector;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(selector, "topCount", 3);
    }

    @Test
    void shouldSelectTopNByScore() {
        List<Article> candidates = List.of(
            buildArticle(1L, 5.0, false),
            buildArticle(2L, 9.0, false),
            buildArticle(3L, 7.0, false),
            buildArticle(4L, 8.0, false),
            buildArticle(5L, 6.0, false)
        );

        List<Article> result = selector.selectTop(candidates);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getTotalScore()).isEqualTo(9.0);
        assertThat(result.get(1).getTotalScore()).isEqualTo(8.0);
        assertThat(result.get(2).getTotalScore()).isEqualTo(7.0);
    }

    @Test
    void shouldExcludeAlreadySentArticles() {
        List<Article> candidates = List.of(
            buildArticle(1L, 10.0, true),
            buildArticle(2L, 9.0, false),
            buildArticle(3L, 8.0, true),
            buildArticle(4L, 7.0, false)
        );

        List<Article> result = selector.selectTop(candidates);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTotalScore()).isEqualTo(9.0);
        assertThat(result.get(1).getTotalScore()).isEqualTo(7.0);
    }

    @Test
    void shouldExcludeArticlesWithNullScore() {
        List<Article> candidates = List.of(
            buildArticle(1L, null, false),
            buildArticle(2L, 8.0, false),
            buildArticle(3L, null, false)
        );

        List<Article> result = selector.selectTop(candidates);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTotalScore()).isEqualTo(8.0);
    }

    @Test
    void shouldReturnFewerThanTopCountWhenNotEnoughCandidates() {
        List<Article> candidates = List.of(
            buildArticle(1L, 8.0, false)
        );

        List<Article> result = selector.selectTop(candidates);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldReturnEmptyForEmptyInput() {
        List<Article> result = selector.selectTop(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenAllSent() {
        List<Article> candidates = List.of(
            buildArticle(1L, 9.0, true),
            buildArticle(2L, 8.0, true)
        );

        List<Article> result = selector.selectTop(candidates);

        assertThat(result).isEmpty();
    }

    private Article buildArticle(Long id, Double totalScore, boolean sent) {
        return Article.builder()
            .id(id)
            .title("Article " + id)
            .url("https://example.com/" + id)
            .source("TEST")
            .totalScore(totalScore)
            .sent(sent)
            .build();
    }
}
