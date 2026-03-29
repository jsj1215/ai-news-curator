package com.curator.ainews.evaluator;

import com.curator.ainews.domain.Article;
import com.curator.ainews.evaluator.dto.EvaluationResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiEvaluator {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${evaluator.score-weights.practicality}")
    private double practicalityWeight;

    @Value("${evaluator.score-weights.freshness}")
    private double freshnessWeight;

    @Value("${evaluator.score-weights.depth}")
    private double depthWeight;

    @Value("${gemini.batch-size}")
    private int batchSize;

    public void evaluateBatch(List<Article> articles) {
        if (articles.isEmpty()) return;

        for (int i = 0; i < articles.size(); i += batchSize) {
            List<Article> batch = articles.subList(i, Math.min(i + batchSize, articles.size()));
            try {
                evaluateSingleBatch(batch);
            } catch (Exception e) {
                log.error("Batch evaluation failed, falling back to individual evaluation", e);
                evaluateIndividually(batch);
            }
        }
    }

    private void evaluateSingleBatch(List<Article> batch) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 백엔드 개발자를 위한 AI 뉴스 큐레이터입니다.\n");
        prompt.append("아래 기사들을 각각 평가해주세요.\n\n");
        prompt.append("평가 기준 (각 1-10점):\n");
        prompt.append("- practicalityScore: 백엔드 개발자가 실무에 적용할 수 있는 정도\n");
        prompt.append("- freshnessScore: 새로운 정보/관점을 제공하는 정도\n");
        prompt.append("- depthScore: 기술적 깊이와 분석의 충실도\n\n");
        prompt.append("반드시 JSON 배열만 응답해주세요 (마크다운 코드블록 없이):\n");
        prompt.append("[{\"index\": 0, \"practicalityScore\": 8, \"freshnessScore\": 7, \"depthScore\": 6}, ...]\n\n");
        prompt.append("기사 목록:\n");

        for (int i = 0; i < batch.size(); i++) {
            Article article = batch.get(i);
            prompt.append(String.format("[%d] 제목: %s\n", i, article.getTitle()));
            String content = article.getContent() != null
                ? article.getContent().substring(0, Math.min(article.getContent().length(), 500))
                : "(내용 없음)";
            prompt.append(String.format("    내용: %s\n    출처: %s\n\n", content, article.getSource()));
        }

        String response = geminiClient.call(prompt.toString());
        if (response == null) {
            log.warn("Gemini returned null, assigning default scores");
            assignDefaultScores(batch);
            return;
        }

        try {
            String cleanJson = cleanJsonResponse(response);
            List<EvaluationResult> results = objectMapper.readValue(cleanJson,
                new TypeReference<List<EvaluationResult>>() {});

            for (EvaluationResult result : results) {
                if (result.getIndex() >= 0 && result.getIndex() < batch.size()) {
                    Article article = batch.get(result.getIndex());
                    article.setPracticalityScore(clamp(result.getPracticalityScore()));
                    article.setFreshnessScore(clamp(result.getFreshnessScore()));
                    article.setDepthScore(clamp(result.getDepthScore()));
                    article.setTotalScore(calculateTotal(article));
                }
            }

            // Assign default scores for any articles that weren't in the response
            for (Article article : batch) {
                if (article.getTotalScore() == null) {
                    assignDefaultScore(article);
                }
            }

            log.info("Batch evaluation completed for {} articles", batch.size());
        } catch (Exception e) {
            log.error("Failed to parse batch evaluation response, falling back to individual", e);
            evaluateIndividually(batch);
        }
    }

    private void evaluateIndividually(List<Article> articles) {
        for (Article article : articles) {
            if (article.getTotalScore() != null) continue;

            try {
                String prompt = String.format(
                    "백엔드 개발자 관점에서 아래 기사를 평가해주세요. JSON만 응답 (마크다운 없이):\n" +
                    "{\"practicalityScore\": 8, \"freshnessScore\": 7, \"depthScore\": 6}\n\n" +
                    "제목: %s\n내용: %s\n출처: %s",
                    article.getTitle(),
                    article.getContent() != null ? article.getContent().substring(0, Math.min(article.getContent().length(), 500)) : "(없음)",
                    article.getSource()
                );

                String response = geminiClient.call(prompt);
                if (response != null) {
                    String cleanJson = cleanJsonResponse(response);
                    EvaluationResult result = objectMapper.readValue(cleanJson, EvaluationResult.class);
                    article.setPracticalityScore(clamp(result.getPracticalityScore()));
                    article.setFreshnessScore(clamp(result.getFreshnessScore()));
                    article.setDepthScore(clamp(result.getDepthScore()));
                    article.setTotalScore(calculateTotal(article));
                } else {
                    assignDefaultScore(article);
                }
            } catch (Exception e) {
                log.warn("Individual evaluation failed for [{}], assigning default", article.getTitle());
                assignDefaultScore(article);
            }
        }
    }

    /**
     * Defensive JSON cleaning:
     * 1. Remove markdown code blocks
     * 2. Extract JSON array or object
     */
    private String cleanJsonResponse(String response) {
        // Remove markdown code blocks
        String cleaned = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

        // Try to extract JSON array
        Pattern arrayPattern = Pattern.compile("\\[.*]", Pattern.DOTALL);
        Matcher arrayMatcher = arrayPattern.matcher(cleaned);
        if (arrayMatcher.find()) {
            return arrayMatcher.group();
        }

        // Try to extract JSON object
        Pattern objectPattern = Pattern.compile("\\{.*}", Pattern.DOTALL);
        Matcher objectMatcher = objectPattern.matcher(cleaned);
        if (objectMatcher.find()) {
            return objectMatcher.group();
        }

        return cleaned;
    }

    private double clamp(double score) {
        return Math.max(0, Math.min(10, score));
    }

    private double calculateTotal(Article article) {
        return article.getPracticalityScore() * practicalityWeight
            + article.getFreshnessScore() * freshnessWeight
            + article.getDepthScore() * depthWeight;
    }

    private void assignDefaultScores(List<Article> articles) {
        articles.forEach(this::assignDefaultScore);
    }

    private void assignDefaultScore(Article article) {
        article.setPracticalityScore(5.0);
        article.setFreshnessScore(5.0);
        article.setDepthScore(5.0);
        article.setTotalScore(5.0);
    }
}
