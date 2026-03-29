package com.curator.ainews.evaluator.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationResult {
    private int index;
    private double practicalityScore;
    private double freshnessScore;
    private double depthScore;
}
