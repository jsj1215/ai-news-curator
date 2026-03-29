package com.curator.ainews.notification;

import com.curator.ainews.domain.Article;
import com.curator.ainews.domain.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramNotifier {

    private final RestTemplate restTemplate;
    private final MessageFormatter messageFormatter;
    private final ArticleRepository articleRepository;

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.chat-id}")
    private String chatId;

    public boolean sendTopArticles(List<Article> articles) {
        if (articles.isEmpty()) {
            log.info("No articles to send");
            return false;
        }

        if (botToken == null || botToken.isEmpty() || chatId == null || chatId.isEmpty()) {
            log.warn("Telegram bot token or chat ID not configured. Skipping notification.");
            return false;
        }

        String message = messageFormatter.format(articles);

        try {
            String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);

            Map<String, Object> body = Map.of(
                "chat_id", chatId,
                "text", message,
                "parse_mode", "HTML",
                "disable_web_page_preview", true
            );

            restTemplate.postForObject(url, body, String.class);

            // Mark articles as sent
            LocalDateTime now = LocalDateTime.now();
            for (Article article : articles) {
                article.setSent(true);
                article.setSentAt(now);
                articleRepository.save(article);
            }

            log.info("Telegram notification sent: {} articles", articles.size());
            return true;
        } catch (Exception e) {
            log.error("Telegram notification failed: {}", e.getMessage());
            return false;
        }
    }
}
