package com.curator.ainews.pipeline;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PipelineResult {
    private final String status;
    private final int collected;
    private final int filtered;
    private final int evaluated;
    private final int sent;
    private final String error;

    public static PipelineResult success(int collected, int filtered, int evaluated, int sent) {
        return new PipelineResult("SUCCESS", collected, filtered, evaluated, sent, null);
    }

    public static PipelineResult skipped() {
        return new PipelineResult("SKIPPED", 0, 0, 0, 0, "Pipeline already running");
    }

    public static PipelineResult failure(String error) {
        return new PipelineResult("FAILURE", 0, 0, 0, 0, error);
    }
}
