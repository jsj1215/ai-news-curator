package com.curator.ainews.evaluator;

import com.curator.ainews.domain.Article;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiEvaluatorTest {

    @Mock
    private GeminiClient geminiClient;

    private GeminiEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new GeminiEvaluator(geminiClient);
        ReflectionTestUtils.setField(evaluator, "practicalityWeight", 0.4);
        ReflectionTestUtils.setField(evaluator, "freshnessWeight", 0.3);
        ReflectionTestUtils.setField(evaluator, "depthWeight", 0.3);
        ReflectionTestUtils.setField(evaluator, "batchSize", 5);
    }

    @Test
    void shouldParseBatchEvaluationResponse() {
        String jsonResponse = "[{\"index\": 0, \"practicalityScore\": 8, \"freshnessScore\": 7, \"depthScore\": 6}]";
        when(geminiClient.call(anyString())).thenReturn(jsonResponse);

        List<Article> articles = new ArrayList<>();
        articles.add(buildArticle("Test Article"));

        evaluator.evaluateBatch(articles);

        assertThat(articles.get(0).getPracticalityScore()).isEqualTo(8.0);
        assertThat(articles.get(0).getFreshnessScore()).isEqualTo(7.0);
        assertThat(articles.get(0).getDepthScore()).isEqualTo(6.0);
        assertThat(articles.get(0).getTotalScore()).isEqualTo(8 * 0.4 + 7 * 0.3 + 6 * 0.3);
    }

    @Test
    void shouldHandleMarkdownWrappedJson() {
        String response = "```json\n[{\"index\": 0, \"practicalityScore\": 9, \"freshnessScore\": 8, \"depthScore\": 7}]\n```";
        when(geminiClient.call(anyString())).thenReturn(response);

        List<Article> articles = new ArrayList<>();
        articles.add(buildArticle("Test"));

        evaluator.evaluateBatch(articles);

        assertThat(articles.get(0).getPracticalityScore()).isEqualTo(9.0);
    }

    @Test
    void shouldAssignDefaultScoresWhenGeminiReturnsNull() {
        when(geminiClient.call(anyString())).thenReturn(null);

        List<Article> articles = new ArrayList<>();
        articles.add(buildArticle("Test"));

        evaluator.evaluateBatch(articles);

        assertThat(articles.get(0).getPracticalityScore()).isEqualTo(5.0);
        assertThat(articles.get(0).getFreshnessScore()).isEqualTo(5.0);
        assertThat(articles.get(0).getDepthScore()).isEqualTo(5.0);
        assertThat(articles.get(0).getTotalScore()).isEqualTo(5.0);
    }

    @Test
    void shouldFallbackToIndividualOnInvalidBatchJson() {
        // First call (batch) returns invalid, second call (individual) returns valid
        when(geminiClient.call(anyString()))
            .thenReturn("invalid json")
            .thenReturn("{\"practicalityScore\": 7, \"freshnessScore\": 6, \"depthScore\": 5}");

        List<Article> articles = new ArrayList<>();
        articles.add(buildArticle("Test"));

        evaluator.evaluateBatch(articles);

        assertThat(articles.get(0).getTotalScore()).isNotNull();
    }

    @Test
    void shouldClampScoresWithinRange() {
        String jsonResponse = "[{\"index\": 0, \"practicalityScore\": 15, \"freshnessScore\": -3, \"depthScore\": 10}]";
        when(geminiClient.call(anyString())).thenReturn(jsonResponse);

        List<Article> articles = new ArrayList<>();
        articles.add(buildArticle("Test"));

        evaluator.evaluateBatch(articles);

        assertThat(articles.get(0).getPracticalityScore()).isEqualTo(10.0);
        assertThat(articles.get(0).getFreshnessScore()).isEqualTo(0.0);
        assertThat(articles.get(0).getDepthScore()).isEqualTo(10.0);
    }

    @Test
    void shouldHandleEmptyArticleList() {
        evaluator.evaluateBatch(new ArrayList<>());
        verify(geminiClient, never()).call(anyString());
    }

    @Test
    void shouldProcessMultipleBatches() {
        String batch1 = "[{\"index\":0,\"practicalityScore\":8,\"freshnessScore\":7,\"depthScore\":6}," +
                         "{\"index\":1,\"practicalityScore\":7,\"freshnessScore\":6,\"depthScore\":5}," +
                         "{\"index\":2,\"practicalityScore\":6,\"freshnessScore\":5,\"depthScore\":4}," +
                         "{\"index\":3,\"practicalityScore\":5,\"freshnessScore\":4,\"depthScore\":3}," +
                         "{\"index\":4,\"practicalityScore\":4,\"freshnessScore\":3,\"depthScore\":2}]";
        String batch2 = "[{\"index\":0,\"practicalityScore\":9,\"freshnessScore\":8,\"depthScore\":7}]";

        when(geminiClient.call(anyString())).thenReturn(batch1).thenReturn(batch2);

        List<Article> articles = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            articles.add(buildArticle("Article " + i));
        }

        evaluator.evaluateBatch(articles);

        verify(geminiClient, times(2)).call(anyString());
        assertThat(articles.get(0).getPracticalityScore()).isEqualTo(8.0);
        assertThat(articles.get(5).getPracticalityScore()).isEqualTo(9.0);
    }

    @Test
    void shouldHandleArticleWithNullContent() {
        String jsonResponse = "[{\"index\": 0, \"practicalityScore\": 7, \"freshnessScore\": 6, \"depthScore\": 5}]";
        when(geminiClient.call(anyString())).thenReturn(jsonResponse);

        List<Article> articles = new ArrayList<>();
        articles.add(Article.builder()
            .title("No Content Article")
            .url("https://example.com/no-content")
            .source("TEST")
            .content(null)
            .build());

        evaluator.evaluateBatch(articles);

        assertThat(articles.get(0).getTotalScore()).isNotNull();
    }

    private Article buildArticle(String title) {
        return Article.builder()
            .title(title)
            .url("https://example.com/" + title.hashCode())
            .source("TEST")
            .content("Some test content for evaluation")
            .build();
    }
}
