package com.curator.ainews.selector;

import com.curator.ainews.domain.Article;
import com.curator.ainews.domain.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopArticleSelector {

    private final ArticleRepository articleRepository;

    @Value("${pipeline.top-count}")
    private int topCount;

    public List<Article> selectTop(List<Article> candidates) {
        List<Article> top = candidates.stream()
            .filter(a -> a.getTotalScore() != null)
            .filter(a -> !Boolean.TRUE.equals(a.getSent()))
            .sorted((a, b) -> Double.compare(
                b.getTotalScore() != null ? b.getTotalScore() : 0,
                a.getTotalScore() != null ? a.getTotalScore() : 0))
            .limit(topCount)
            .collect(Collectors.toList());

        log.info("Selected top {} articles from {} candidates", top.size(), candidates.size());
        return top;
    }
}
