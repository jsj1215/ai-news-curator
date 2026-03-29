package com.curator.ainews.collector;

import com.curator.ainews.collector.dto.RawArticle;
import com.curator.ainews.domain.Article;
import com.curator.ainews.domain.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollectorOrchestrator {

    private final List<ArticleCollector> collectors;
    private final ArticleRepository articleRepository;

    public List<Article> collectAll() {
        List<RawArticle> rawArticles = new ArrayList<>();

        for (ArticleCollector collector : collectors) {
            try {
                List<RawArticle> collected = collector.collect();
                rawArticles.addAll(collected);
                log.info("Collector [{}] returned {} articles", collector.getSourceName(), collected.size());
            } catch (Exception e) {
                log.error("Collector [{}] failed: {}", collector.getSourceName(), e.getMessage());
            }
        }

        log.info("Total raw articles collected: {}", rawArticles.size());
        return deduplicateAndSave(rawArticles);
    }

    private List<Article> deduplicateAndSave(List<RawArticle> rawArticles) {
        List<Article> newArticles = new ArrayList<>();

        for (RawArticle raw : rawArticles) {
            if (raw.getUrl() == null || raw.getUrl().isEmpty()) continue;

            if (articleRepository.existsByUrl(raw.getUrl())) {
                log.debug("Duplicate URL skipped: {}", raw.getUrl());
                continue;
            }

            Article article = Article.builder()
                .title(raw.getTitle())
                .url(raw.getUrl())
                .source(raw.getSource())
                .content(raw.getContent())
                .publishedAt(raw.getPublishedAt())
                .build();

            try {
                Article saved = articleRepository.save(article);
                newArticles.add(saved);
            } catch (Exception e) {
                log.warn("Failed to save article [{}]: {}", raw.getUrl(), e.getMessage());
            }
        }

        log.info("New articles saved: {} (deduplicated from {})", newArticles.size(), rawArticles.size());
        return newArticles;
    }
}
