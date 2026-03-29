package com.curator.ainews.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleRepository articleRepository;

    @GetMapping("/today")
    public ResponseEntity<List<Map<String, Object>>> getToday() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<Article> articles = articleRepository.findSentArticlesByDateRange(startOfDay, endOfDay);
        return ResponseEntity.ok(groupByTime(articles));
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<Article> articles = articleRepository.findSentArticlesByDateRange(start, end);
        return ResponseEntity.ok(groupByTime(articles));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Article> getArticle(@PathVariable Long id) {
        return articleRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/dates")
    public ResponseEntity<List<LocalDate>> getDates() {
        List<LocalDate> dates = articleRepository.findDistinctSentDates()
            .stream()
            .map(java.sql.Date::toLocalDate)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dates);
    }

    private List<Map<String, Object>> groupByTime(List<Article> articles) {
        return articles.stream()
            .collect(Collectors.groupingBy(
                a -> a.getSentAt() != null ? a.getSentAt().withMinute(0).withSecond(0).withNano(0) : LocalDateTime.MIN
            ))
            .entrySet().stream()
            .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
            .map(entry -> Map.<String, Object>of(
                "time", entry.getKey().toString(),
                "articles", entry.getValue()
            ))
            .collect(Collectors.toList());
    }
}
