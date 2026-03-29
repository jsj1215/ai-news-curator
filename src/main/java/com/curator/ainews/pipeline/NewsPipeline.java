package com.curator.ainews.pipeline;

import com.curator.ainews.collector.CollectorOrchestrator;
import com.curator.ainews.domain.Article;
import com.curator.ainews.domain.ArticleRepository;
import com.curator.ainews.evaluator.GeminiEvaluator;
import com.curator.ainews.evaluator.GeminiSummarizer;
import com.curator.ainews.filter.RuleBasedFilter;
import com.curator.ainews.notification.TelegramNotifier;
import com.curator.ainews.selector.TopArticleSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsPipeline {

    private final ReentrantLock pipelineLock = new ReentrantLock();

    private final CollectorOrchestrator collectorOrchestrator;
    private final RuleBasedFilter ruleBasedFilter;
    private final GeminiEvaluator geminiEvaluator;
    private final GeminiSummarizer geminiSummarizer;
    private final TopArticleSelector topArticleSelector;
    private final TelegramNotifier telegramNotifier;
    private final ArticleRepository articleRepository;

    public PipelineResult execute() {
        if (!pipelineLock.tryLock()) {
            log.warn("Pipeline is already running. Skipping this execution.");
            return PipelineResult.skipped();
        }

        try {
            log.info("=== Pipeline execution started ===");
            long startTime = System.currentTimeMillis();

            // 1. COLLECT + DEDUP
            List<Article> newArticles = collectorOrchestrator.collectAll();
            log.info("Step 1 - Collected: {} new articles", newArticles.size());

            // 2. FILTER
            List<Article> filtered = ruleBasedFilter.filter(newArticles);
            log.info("Step 2 - Filtered: {} articles passed", filtered.size());

            // 3. EVALUATE
            geminiEvaluator.evaluateBatch(filtered);
            articleRepository.saveAll(filtered);
            log.info("Step 3 - Evaluated: {} articles scored", filtered.size());

            // 4. SELECT top N
            List<Article> top = topArticleSelector.selectTop(filtered);
            log.info("Step 4 - Selected: {} top articles", top.size());

            // 5. SUMMARIZE (only top articles to save API calls)
            geminiSummarizer.summarize(top);
            articleRepository.saveAll(top);
            log.info("Step 5 - Summarized: {} articles", top.size());

            // 6. SEND via Telegram
            boolean sent = telegramNotifier.sendTopArticles(top);
            log.info("Step 6 - Telegram sent: {}", sent);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("=== Pipeline execution completed in {}ms ===", elapsed);

            return PipelineResult.success(newArticles.size(), filtered.size(), filtered.size(), top.size());
        } catch (Exception e) {
            log.error("Pipeline execution failed", e);
            return PipelineResult.failure(e.getMessage());
        } finally {
            pipelineLock.unlock();
        }
    }

    public boolean isRunning() {
        return pipelineLock.isLocked();
    }
}
