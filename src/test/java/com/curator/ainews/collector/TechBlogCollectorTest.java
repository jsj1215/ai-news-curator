package com.curator.ainews.collector;

import com.curator.ainews.collector.dto.RawArticle;
import com.curator.ainews.config.CollectorConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TechBlogCollectorTest {

    @Test
    void getSourceName_returnsTechBlog() {
        CollectorConfig config = new CollectorConfig();
        TechBlogCollector collector = new TechBlogCollector(config);

        assertThat(collector.getSourceName()).isEqualTo("TECH_BLOG");
    }

    @Test
    void collect_returnsEmptyWhenNoSources() {
        CollectorConfig config = new CollectorConfig();
        config.setSources(List.of());

        TechBlogCollector collector = new TechBlogCollector(config);
        List<RawArticle> articles = collector.collect();

        assertThat(articles).isEmpty();
    }

    @Test
    void collect_skipsSourcesWithEmptyFeedUrl() {
        CollectorConfig config = new CollectorConfig();
        CollectorConfig.BlogSource source = new CollectorConfig.BlogSource();
        source.setName("test-blog");
        source.setFeedUrl("");
        config.setSources(List.of(source));

        TechBlogCollector collector = new TechBlogCollector(config);
        List<RawArticle> articles = collector.collect();

        assertThat(articles).isEmpty();
    }

    @Test
    void collect_skipsSourcesWithNullFeedUrl() {
        CollectorConfig config = new CollectorConfig();
        CollectorConfig.BlogSource source = new CollectorConfig.BlogSource();
        source.setName("test-blog");
        source.setFeedUrl(null);
        config.setSources(List.of(source));

        TechBlogCollector collector = new TechBlogCollector(config);
        List<RawArticle> articles = collector.collect();

        assertThat(articles).isEmpty();
    }

    @Test
    void collect_handlesInvalidFeedUrlGracefully() {
        CollectorConfig config = new CollectorConfig();
        CollectorConfig.BlogSource source = new CollectorConfig.BlogSource();
        source.setName("invalid-blog");
        source.setFeedUrl("https://invalid.example.com/nonexistent-feed");
        config.setSources(List.of(source));

        TechBlogCollector collector = new TechBlogCollector(config);

        // Should not throw, just log error and return empty
        List<RawArticle> articles = collector.collect();
        assertThat(articles).isNotNull();
    }
}
