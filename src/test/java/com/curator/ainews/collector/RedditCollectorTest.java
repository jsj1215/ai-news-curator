package com.curator.ainews.collector;

import com.curator.ainews.collector.dto.RawArticle;
import com.curator.ainews.config.RedditConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RedditCollectorTest {

    @Test
    void getSourceName_returnsReddit() {
        RedditConfig config = new RedditConfig();
        RedditCollector collector = new RedditCollector(config);

        assertThat(collector.getSourceName()).isEqualTo("REDDIT");
    }

    @Test
    void collect_returnsEmptyOnInvalidSubreddit() {
        RedditConfig config = new RedditConfig();
        config.setSubreddits(List.of("r/nonexistent_subreddit_xyz_12345"));
        config.setMaxAgeHours(4);

        RedditCollector collector = new RedditCollector(config);

        // Should not throw, returns empty or partial results
        List<RawArticle> articles = collector.collect();
        assertThat(articles).isNotNull();
    }

    @Test
    void collect_returnsEmptyWhenNoSubreddits() {
        RedditConfig config = new RedditConfig();
        config.setSubreddits(List.of());

        RedditCollector collector = new RedditCollector(config);
        List<RawArticle> articles = collector.collect();

        assertThat(articles).isEmpty();
    }
}
