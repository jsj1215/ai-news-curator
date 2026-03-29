package com.curator.ainews.collector;

import com.curator.ainews.collector.dto.RawArticle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class HackerNewsCollector implements ArticleCollector {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String query;
    private final int maxResults;
    private final int maxAgeHours;

    public HackerNewsCollector(
            RestTemplate restTemplate,
            @Value("${collector.hacker-news.api-url}") String apiUrl,
            @Value("${collector.hacker-news.query}") String query,
            @Value("${collector.hacker-news.max-results}") int maxResults,
            @Value("${collector.hacker-news.max-age-hours}") int maxAgeHours) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        this.apiUrl = apiUrl;
        this.query = query;
        this.maxResults = maxResults;
        this.maxAgeHours = maxAgeHours;
    }

    @Override
    public String getSourceName() {
        return "HACKER_NEWS";
    }

    @Override
    public List<RawArticle> collect() {
        List<RawArticle> articles = new ArrayList<>();

        try {
            long cutoffTimestamp = Instant.now().minusSeconds(maxAgeHours * 3600L).getEpochSecond();
            String url = String.format("%s?query=%s&tags=story&hitsPerPage=%d&numericFilters=created_at_i>%d",
                apiUrl, query, maxResults, cutoffTimestamp);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode hits = root.get("hits");

            if (hits != null && hits.isArray()) {
                for (JsonNode hit : hits) {
                    String title = getTextOrDefault(hit, "title", "");
                    String articleUrl = getTextOrDefault(hit, "url", "");
                    String storyText = getTextOrDefault(hit, "story_text", "");

                    if (articleUrl.isEmpty()) {
                        articleUrl = "https://news.ycombinator.com/item?id=" + getTextOrDefault(hit, "objectID", "");
                    }

                    if (title.isEmpty()) continue;

                    long createdAtI = hit.has("created_at_i") ? hit.get("created_at_i").asLong() : 0;
                    LocalDateTime publishedAt = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(createdAtI), ZoneId.systemDefault());

                    articles.add(RawArticle.builder()
                        .title(title)
                        .url(articleUrl)
                        .source(getSourceName())
                        .content(storyText)
                        .publishedAt(publishedAt)
                        .build());
                }
            }

            log.info("HackerNews collected {} articles", articles.size());
        } catch (Exception e) {
            log.error("HackerNews collection failed: {}", e.getMessage());
        }

        return articles;
    }

    private String getTextOrDefault(JsonNode node, String field, String defaultValue) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : defaultValue;
    }
}
