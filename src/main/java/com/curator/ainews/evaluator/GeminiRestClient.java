package com.curator.ainews.evaluator;

import com.curator.ainews.domain.ApiCallLog;
import com.curator.ainews.domain.ApiCallLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiRestClient implements GeminiClient {

    private final RestTemplate restTemplate;
    private final ApiCallLogRepository apiCallLogRepository;
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final int dailyLimit;
    private final long rpmDelayMs;

    public GeminiRestClient(
            RestTemplate restTemplate,
            ApiCallLogRepository apiCallLogRepository,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model,
            @Value("${gemini.temperature}") double temperature,
            @Value("${gemini.daily-limit}") int dailyLimit,
            @Value("${gemini.rpm-delay-ms}") long rpmDelayMs) {
        this.restTemplate = restTemplate;
        this.apiCallLogRepository = apiCallLogRepository;
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.dailyLimit = dailyLimit;
        this.rpmDelayMs = rpmDelayMs;
    }

    @Override
    public String call(String prompt) {
        if (!canCall()) {
            log.warn("Gemini daily limit reached ({}). Skipping call.", dailyLimit);
            return null;
        }

        waitForRateLimit();

        String url = String.format(
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
            model, apiKey
        );

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(Map.of("text", prompt))
            )),
            "generationConfig", Map.of(
                "temperature", temperature,
                "maxOutputTokens", 2048
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST,
                new HttpEntity<>(requestBody, headers),
                Map.class
            );

            logApiCall();

            if (response.getBody() != null) {
                List<Map> candidates = (List<Map>) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map content = (Map) candidates.get(0).get("content");
                    List<Map> parts = (List<Map>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
        }

        return null;
    }

    private boolean canCall() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long todayCount = apiCallLogRepository.countByApiNameAndCalledAtAfter("gemini", startOfDay);
        return todayCount < dailyLimit;
    }

    private void waitForRateLimit() {
        try {
            Thread.sleep(rpmDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void logApiCall() {
        apiCallLogRepository.save(ApiCallLog.builder()
            .apiName("gemini")
            .build());
    }
}
