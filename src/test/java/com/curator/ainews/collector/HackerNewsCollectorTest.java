package com.curator.ainews.collector;

import com.curator.ainews.collector.dto.RawArticle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HackerNewsCollectorTest {

    @Mock
    private RestTemplate restTemplate;

    private HackerNewsCollector collector;

    @BeforeEach
    void setUp() {
        collector = new HackerNewsCollector(
                restTemplate,
                "https://hn.algolia.com/api/v1/search_by_date",
                "AI",
                30,
                4
        );
    }

    @Test
    void getSourceName_returnsHackerNews() {
        assertThat(collector.getSourceName()).isEqualTo("HACKER_NEWS");
    }

    @Test
    void collect_parsesApiResponse() {
        long now = Instant.now().getEpochSecond();
        String json = """
                {
                  "hits": [
                    {
                      "title": "Spring AI Released",
                      "url": "https://spring.io/ai",
                      "story_text": "Spring AI is now available",
                      "created_at_i": %d,
                      "objectID": "12345"
                    },
                    {
                      "title": "LLM for Backend",
                      "url": "",
                      "story_text": "",
                      "created_at_i": %d,
                      "objectID": "67890"
                    }
                  ]
                }
                """.formatted(now, now);

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<RawArticle> articles = collector.collect();

        assertThat(articles).hasSize(2);
        assertThat(articles.get(0).getTitle()).isEqualTo("Spring AI Released");
        assertThat(articles.get(0).getUrl()).isEqualTo("https://spring.io/ai");
        assertThat(articles.get(0).getSource()).isEqualTo("HACKER_NEWS");
        // Empty URL should fallback to HN item link
        assertThat(articles.get(1).getUrl()).contains("67890");
    }

    @Test
    void collect_skipsEntriesWithEmptyTitle() {
        long now = Instant.now().getEpochSecond();
        String json = """
                {
                  "hits": [
                    {
                      "title": "",
                      "url": "https://example.com",
                      "created_at_i": %d,
                      "objectID": "111"
                    }
                  ]
                }
                """.formatted(now);

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<RawArticle> articles = collector.collect();

        assertThat(articles).isEmpty();
    }

    @Test
    void collect_returnsEmptyOnApiError() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("API down"));

        List<RawArticle> articles = collector.collect();

        assertThat(articles).isEmpty();
    }

    @Test
    void collect_handlesNullHits() {
        String json = """
                {
                  "hits": null
                }
                """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<RawArticle> articles = collector.collect();

        assertThat(articles).isEmpty();
    }
}
