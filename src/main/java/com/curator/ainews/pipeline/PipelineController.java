package com.curator.ainews.pipeline;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final NewsPipeline newsPipeline;

    @PostMapping("/run")
    public ResponseEntity<PipelineResult> runManually() {
        PipelineResult result = newsPipeline.execute();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "running", newsPipeline.isRunning()
        ));
    }
}
