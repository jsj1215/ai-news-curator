package com.curator.ainews.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsPipeline {

    private final ReentrantLock pipelineLock = new ReentrantLock();

    // Dependencies will be injected as they are implemented in later days
    // private final CollectorOrchestrator collectorOrchestrator;
    // private final RuleBasedFilter ruleBasedFilter;
    // private final GeminiEvaluator geminiEvaluator;
    // private final GeminiSummarizer geminiSummarizer;
    // private final TopArticleSelector topArticleSelector;
    // private final TelegramNotifier telegramNotifier;

    public PipelineResult execute() {
        if (!pipelineLock.tryLock()) {
            log.warn("Pipeline is already running. Skipping this execution.");
            return PipelineResult.skipped();
        }

        try {
            log.info("=== Pipeline execution started ===");
            long startTime = System.currentTimeMillis();

            // TODO: Phase 2 에서 각 단계 구현
            // 1. COLLECT
            // 2. DEDUP
            // 3. FILTER
            // 4. EVALUATE
            // 5. SUMMARIZE
            // 6. SELECT
            // 7. SEND

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("=== Pipeline execution completed in {}ms ===", elapsed);

            return PipelineResult.success(0, 0, 0, 0);
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
