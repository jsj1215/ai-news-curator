package com.curator.ainews.evaluator;

import com.curator.ainews.domain.Article;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiSummarizerTest {

    @Mock
    private GeminiClient geminiClient;

    @InjectMocks
    private GeminiSummarizer summarizer;

    @Test
    void shouldSummarizeArticle() {
        when(geminiClient.call(anyString())).thenReturn("Spring AI 1.0이 정식 출시되었습니다.");

        List<Article> articles = new ArrayList<>();
        articles.add(buildArticle("Spring AI 1.0 Released", null));

        summarizer.summarize(articles);

        assertThat(articles.get(0).getSummary()).isEqualTo("Spring AI 1.0이 정식 출시되었습니다.");
    }

    @Test
    void shouldSkipAlreadySummarizedArticle() {
        List<Article> articles = new ArrayList<>();
        articles.add(buildArticle("Test", "이미 요약됨"));

        summarizer.summarize(articles);

        verify(geminiClient, never()).call(anyString());
        assertThat(articles.get(0).getSummary()).isEqualTo("이미 요약됨");
    }

    @Test
    void shouldSetFallbackWhenGeminiReturnsNull() {
        when(geminiClient.call(anyString())).thenReturn(null);

        List<Article> articles = new ArrayList<>();
        articles.add(buildArticle("Test", null));

        summarizer.summarize(articles);

        assertThat(articles.get(0).getSummary()).isEqualTo("요약을 생성할 수 없습니다.");
    }

    @Test
    void shouldTruncateLongSummary() {
        String longSummary = "A".repeat(600);
        when(geminiClient.call(anyString())).thenReturn(longSummary);

        List<Article> articles = new ArrayList<>();
        articles.add(buildArticle("Test", null));

        summarizer.summarize(articles);

        assertThat(articles.get(0).getSummary().length()).isLessThanOrEqualTo(500);
        assertThat(articles.get(0).getSummary()).endsWith("...");
    }

    @Test
    void shouldCleanMarkdownFromSummary() {
        // GeminiSummarizer uses replaceAll("```.*?```", "") which doesn't match across lines
        // So single-line code blocks are cleaned, but multi-line ones are cleaned by removing ``` markers
        when(geminiClient.call(anyString())).thenReturn("```요약 내용입니다.```");

        List<Article> articles = new ArrayList<>();
        articles.add(buildArticle("Test", null));

        summarizer.summarize(articles);

        assertThat(articles.get(0).getSummary()).doesNotContain("```");
    }

    @Test
    void shouldHandleExceptionGracefully() {
        when(geminiClient.call(anyString())).thenThrow(new RuntimeException("API error"));

        List<Article> articles = new ArrayList<>();
        articles.add(buildArticle("Test", null));

        summarizer.summarize(articles);

        assertThat(articles.get(0).getSummary()).isEqualTo("요약을 생성할 수 없습니다.");
    }

    @Test
    void shouldHandleArticleWithNullContent() {
        when(geminiClient.call(anyString())).thenReturn("제목 기반 요약입니다.");

        List<Article> articles = new ArrayList<>();
        Article article = Article.builder()
            .title("Test Article")
            .url("https://example.com/1")
            .source("TEST")
            .content(null)
            .build();
        articles.add(article);

        summarizer.summarize(articles);

        assertThat(articles.get(0).getSummary()).isEqualTo("제목 기반 요약입니다.");
    }

    private Article buildArticle(String title, String summary) {
        return Article.builder()
            .title(title)
            .url("https://example.com/" + title.hashCode())
            .source("TEST")
            .content("Test content")
            .summary(summary)
            .build();
    }
}
