package com.curator.ainews.scheduler;

import com.curator.ainews.pipeline.NewsPipeline;
import com.curator.ainews.pipeline.PipelineResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PipelineScheduler {

    private final NewsPipeline newsPipeline;

    @Scheduled(cron = "${pipeline.schedule-cron}")
    public void runPipeline() {
        log.info("Scheduled pipeline execution triggered");
        PipelineResult result = newsPipeline.execute();
        log.info("Scheduled pipeline result: {}", result.getStatus());
    }
}
