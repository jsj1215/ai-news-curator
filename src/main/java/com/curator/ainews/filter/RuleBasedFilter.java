package com.curator.ainews.filter;

import com.curator.ainews.config.FilterConfig;
import com.curator.ainews.domain.Article;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleBasedFilter {

    private final FilterConfig filterConfig;

    private static final Set<String> KOREAN_BLOG_SOURCES = Set.of(
        "NAVER_D2", "KAKAO", "COUPANG", "WOOWAHAN", "LINE", "DAANGN",
        "TOSS", "OLIVEYOUNG", "GANGNAMUNNI"
    );

    public List<Article> filter(List<Article> articles) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(filterConfig.getMaxAgeHours());

        Set<String> aiKeywords = filterConfig.getKeywords().getAi().stream()
            .map(String::toLowerCase).collect(Collectors.toSet());
        Set<String> backendKeywords = filterConfig.getKeywords().getBackend().stream()
            .map(String::toLowerCase).collect(Collectors.toSet());
        Set<String> excludeKeywords = filterConfig.getKeywords().getExclude().stream()
            .map(String::toLowerCase).collect(Collectors.toSet());

        List<Article> filtered = articles.stream()
            .filter(a -> a.getTitle() != null && a.getTitle().length() >= filterConfig.getMinTitleLength())
            .filter(a -> a.getPublishedAt() == null || a.getPublishedAt().isAfter(cutoff))
            .filter(a -> !containsAny(buildSearchText(a), excludeKeywords))
            .filter(a -> {
                String text = buildSearchText(a);
                boolean hasAi = containsAny(text, aiKeywords);
                boolean hasBackend = containsAny(text, backendKeywords);
                boolean isKoreanBlog = KOREAN_BLOG_SOURCES.contains(a.getSource());
                return (hasAi && hasBackend) || (isKoreanBlog && hasAi);
            })
            .collect(Collectors.toList());

        log.info("Filter: {} -> {} articles passed", articles.size(), filtered.size());
        return filtered;
    }

    private String buildSearchText(Article article) {
        StringBuilder sb = new StringBuilder();
        if (article.getTitle() != null) sb.append(article.getTitle()).append(" ");
        if (article.getContent() != null) sb.append(article.getContent());
        return sb.toString().toLowerCase();
    }

    private boolean containsAny(String text, Set<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }
}
