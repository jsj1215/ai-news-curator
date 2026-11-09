package com.curator.ainews.web;

import com.curator.ainews.domain.ApiCallLogRepository;
import com.curator.ainews.domain.Article;
import com.curator.ainews.domain.ArticleRepository;
import com.curator.ainews.pipeline.NewsPipeline;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final ArticleRepository articleRepository;
    private final ApiCallLogRepository apiCallLogRepository;
    private final NewsPipeline newsPipeline;

    @GetMapping("/")
    public String dashboard(Model model) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<Article> todayArticles = articleRepository.findSentArticlesByDateRange(startOfDay, endOfDay);

        // Group by hour
        Map<Integer, List<Article>> groupedByHour = todayArticles.stream()
                .filter(a -> a.getSentAt() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getSentAt().getHour(),
                        TreeMap::new,
                        Collectors.toList()
                ));

        model.addAttribute("todayArticles", todayArticles);
        model.addAttribute("groupedByHour", groupedByHour);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("currentPage", "home");
        return "index";
    }

    @GetMapping("/history")
    public String history(@RequestParam(required = false) String date, Model model) {
        LocalDate targetDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.atTime(LocalTime.MAX);

        List<Article> articles = articleRepository.findSentArticlesByDateRange(start, end);
        List<java.sql.Date> availableDates = articleRepository.findDistinctSentDates();

        model.addAttribute("articles", articles);
        model.addAttribute("selectedDate", targetDate);
        model.addAttribute("availableDates", availableDates);
        model.addAttribute("currentPage", "history");
        return "history";
    }

    @GetMapping("/articles/{id}")
    public String articleDetail(@PathVariable Long id, Model model) {
        Article article = articleRepository.findById(id).orElse(null);
        if (article == null) {
            return "error/404";
        }
        model.addAttribute("article", article);
        model.addAttribute("currentPage", "");
        return "article";
    }

    @GetMapping("/admin/pipeline")
    public String pipeline(Model model) {
        boolean running = newsPipeline.isRunning();
        List<Article> recentArticles = articleRepository.findTop10ByOrderByCreatedAtDesc();

        model.addAttribute("isRunning", running);
        model.addAttribute("recentArticles", recentArticles);
        model.addAttribute("currentPage", "pipeline");
        return "pipeline";
    }

    @GetMapping("/admin/stats")
    public String stats(Model model) {
        long totalArticles = articleRepository.count();
        long sentArticles = articleRepository.countBySentTrue();
        Double avgScore = articleRepository.findAverageTotalScore();
        List<Object[]> sourceStats = articleRepository.countBySourceGrouped();
        List<Object[]> sourceAvgScores = articleRepository.findAverageScoreBySource();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayCollected = articleRepository.countByCreatedAtAfter(todayStart);
        long geminiCallsToday = apiCallLogRepository.countByApiNameAndCalledAtAfter("gemini", todayStart);

        model.addAttribute("totalArticles", totalArticles);
        model.addAttribute("sentArticles", sentArticles);
        model.addAttribute("avgScore", avgScore != null ? String.format("%.1f", avgScore) : "N/A");
        model.addAttribute("sourceStats", sourceStats);
        model.addAttribute("sourceAvgScores", sourceAvgScores);
        model.addAttribute("todayCollected", todayCollected);
        model.addAttribute("geminiCallsToday", geminiCallsToday);
        model.addAttribute("currentPage", "stats");
        return "stats";
    }
}
