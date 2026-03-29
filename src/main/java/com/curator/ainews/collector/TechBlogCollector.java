package com.curator.ainews.collector;

import com.curator.ainews.collector.dto.RawArticle;
import com.curator.ainews.config.CollectorConfig;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TechBlogCollector implements ArticleCollector {

    private final CollectorConfig collectorConfig;

    @Override
    public String getSourceName() {
        return "TECH_BLOG";
    }

    @Override
    public List<RawArticle> collect() {
        List<RawArticle> articles = new ArrayList<>();

        for (CollectorConfig.BlogSource source : collectorConfig.getSources()) {
            if (source.getFeedUrl() == null || source.getFeedUrl().isEmpty()) continue;

            try {
                List<RawArticle> blogArticles = collectFromFeed(source.getName(), source.getFeedUrl());
                articles.addAll(blogArticles);
                log.info("TechBlog [{}] collected {} articles", source.getName(), blogArticles.size());
            } catch (Exception e) {
                log.error("TechBlog [{}] collection failed: {}", source.getName(), e.getMessage());
            }
        }

        return articles;
    }

    private List<RawArticle> collectFromFeed(String sourceName, String feedUrl) throws Exception {
        List<RawArticle> articles = new ArrayList<>();

        HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(feedUrl))
            .header("User-Agent", "AI-News-Curator/1.0")
            .GET()
            .build();

        HttpResponse<java.io.InputStream> response = client.send(request,
            HttpResponse.BodyHandlers.ofInputStream());

        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(response.body()));

        LocalDateTime cutoff = LocalDateTime.now().minusHours(collectorConfig.getMaxAgeHours());

        for (SyndEntry entry : feed.getEntries()) {
            LocalDateTime publishedAt = entry.getPublishedDate() != null
                ? entry.getPublishedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                : (entry.getUpdatedDate() != null
                    ? entry.getUpdatedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                    : LocalDateTime.now());

            if (publishedAt.isBefore(cutoff)) continue;

            String title = entry.getTitle();
            String url = entry.getLink();
            String content = "";

            if (entry.getContents() != null && !entry.getContents().isEmpty()) {
                content = entry.getContents().get(0).getValue();
            } else if (entry.getDescription() != null) {
                content = entry.getDescription().getValue();
            }

            if (title == null || title.isEmpty() || url == null) continue;

            String sourceLabel = sourceName != null
                ? sourceName.toUpperCase().replace("-", "_")
                : "UNKNOWN";

            articles.add(RawArticle.builder()
                .title(title)
                .url(url)
                .source(sourceLabel)
                .content(content)
                .publishedAt(publishedAt)
                .build());
        }

        return articles;
    }
}
