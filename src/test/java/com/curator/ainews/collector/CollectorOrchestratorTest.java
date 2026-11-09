package com.curator.ainews.collector;

import com.curator.ainews.collector.dto.RawArticle;
import com.curator.ainews.domain.Article;
import com.curator.ainews.domain.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectorOrchestratorTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleCollector collector1;

    @Mock
    private ArticleCollector collector2;

    private CollectorOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new CollectorOrchestrator(List.of(collector1, collector2), articleRepository);
    }

    @Test
    void shouldCollectFromAllCollectors() {
        when(collector1.getSourceName()).thenReturn("HN");
        when(collector1.collect()).thenReturn(List.of(
            buildRawArticle("Article 1", "https://example.com/1", "HN")
        ));
        when(collector2.getSourceName()).thenReturn("REDDIT");
        when(collector2.collect()).thenReturn(List.of(
            buildRawArticle("Article 2", "https://example.com/2", "REDDIT")
        ));

        when(articleRepository.existsByUrl(anyString())).thenReturn(false);
        when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Article> result = orchestrator.collectAll();

        assertThat(result).hasSize(2);
        verify(collector1).collect();
        verify(collector2).collect();
    }

    @Test
    void shouldDeduplicateByUrl() {
        when(collector1.getSourceName()).thenReturn("HN");
        when(collector1.collect()).thenReturn(List.of(
            buildRawArticle("Article 1", "https://example.com/dup", "HN")
        ));
        when(collector2.getSourceName()).thenReturn("REDDIT");
        when(collector2.collect()).thenReturn(List.of(
            buildRawArticle("Article 1 Copy", "https://example.com/dup", "REDDIT")
        ));

        // First call: not exists, second call: already exists
        when(articleRepository.existsByUrl("https://example.com/dup"))
            .thenReturn(false)
            .thenReturn(true);
        when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Article> result = orchestrator.collectAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldSkipExistingArticles() {
        when(collector1.getSourceName()).thenReturn("HN");
        when(collector1.collect()).thenReturn(List.of(
            buildRawArticle("Old Article", "https://example.com/old", "HN")
        ));
        when(collector2.getSourceName()).thenReturn("REDDIT");
        when(collector2.collect()).thenReturn(List.of());

        when(articleRepository.existsByUrl("https://example.com/old")).thenReturn(true);

        List<Article> result = orchestrator.collectAll();

        assertThat(result).isEmpty();
        verify(articleRepository, never()).save(any());
    }

    @Test
    void shouldSkipArticlesWithNullUrl() {
        when(collector1.getSourceName()).thenReturn("HN");
        when(collector1.collect()).thenReturn(List.of(
            buildRawArticle("No URL", null, "HN"),
            buildRawArticle("Empty URL", "", "HN")
        ));
        when(collector2.getSourceName()).thenReturn("REDDIT");
        when(collector2.collect()).thenReturn(List.of());

        List<Article> result = orchestrator.collectAll();

        assertThat(result).isEmpty();
        verify(articleRepository, never()).save(any());
    }

    @Test
    void shouldContinueWhenOneCollectorFails() {
        when(collector1.getSourceName()).thenReturn("HN");
        when(collector1.collect()).thenThrow(new RuntimeException("API down"));
        when(collector2.getSourceName()).thenReturn("REDDIT");
        when(collector2.collect()).thenReturn(List.of(
            buildRawArticle("Article", "https://example.com/ok", "REDDIT")
        ));

        when(articleRepository.existsByUrl(anyString())).thenReturn(false);
        when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Article> result = orchestrator.collectAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldReturnEmptyWhenAllCollectorsFail() {
        when(collector1.getSourceName()).thenReturn("HN");
        when(collector1.collect()).thenThrow(new RuntimeException("fail"));
        when(collector2.getSourceName()).thenReturn("REDDIT");
        when(collector2.collect()).thenThrow(new RuntimeException("fail"));

        List<Article> result = orchestrator.collectAll();

        assertThat(result).isEmpty();
    }

    private RawArticle buildRawArticle(String title, String url, String source) {
        return RawArticle.builder()
            .title(title)
            .url(url)
            .source(source)
            .content("Content")
            .publishedAt(LocalDateTime.now())
            .build();
    }
}
