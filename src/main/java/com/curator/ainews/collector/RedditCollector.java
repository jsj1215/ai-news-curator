package com.curator.ainews.collector;

import com.curator.ainews.collector.dto.RawArticle;
import com.curator.ainews.config.RedditConfig;
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
public class RedditCollector implements ArticleCollector {

    private final RedditConfig redditConfig;

    @Override
    public String getSourceName() {
        return "REDDIT";
    }

    @Override
    public List<RawArticle> collect() {
        List<RawArticle> articles = new ArrayList<>();

        for (String subreddit : redditConfig.getSubreddits()) {
            try {
                String feedUrl = String.format("https://www.reddit.com/%s/.rss", subreddit);

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

                LocalDateTime cutoff = LocalDateTime.now().minusHours(redditConfig.getMaxAgeHours());

                for (SyndEntry entry : feed.getEntries()) {
                    LocalDateTime publishedAt = entry.getPublishedDate() != null
                        ? entry.getPublishedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        : LocalDateTime.now();

                    if (publishedAt.isBefore(cutoff)) continue;

                    String title = entry.getTitle();
                    String url = entry.getLink();
                    String content = entry.getDescription() != null ? entry.getDescription().getValue() : "";

                    if (title == null || title.isEmpty() || url == null) continue;

                    articles.add(RawArticle.builder()
                        .title(title)
                        .url(url)
                        .source("REDDIT_" + subreddit.replace("r/", "").toUpperCase())
                        .content(content)
                        .publishedAt(publishedAt)
                        .build());
                }

                log.info("Reddit [{}] collected {} entries", subreddit, articles.size());
            } catch (Exception e) {
                log.error("Reddit [{}] collection failed: {}", subreddit, e.getMessage());
            }
        }

        return articles;
    }
}
