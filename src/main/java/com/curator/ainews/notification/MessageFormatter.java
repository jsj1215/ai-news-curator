package com.curator.ainews.notification;

import com.curator.ainews.domain.Article;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class MessageFormatter {

    private static final int TELEGRAM_MAX_LENGTH = 4096;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public String format(List<Article> articles) {
        StringBuilder sb = new StringBuilder();
        String time = LocalDateTime.now().format(TIME_FORMAT);

        sb.append(String.format("🔥 Top %d AI for Backend (%s)\n\n", articles.size(), time));

        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);

            sb.append(String.format("%d. %s\n", i + 1, article.getTitle()));
            sb.append(String.format("   📝 %s\n", truncate(article.getSummary(), 200)));
            sb.append(String.format("   ⭐ %.1f | 📌 %s\n",
                article.getTotalScore() != null ? article.getTotalScore() : 0.0,
                article.getSource()));
            sb.append(String.format("   🔗 %s\n", article.getUrl()));

            if (i < articles.size() - 1) {
                sb.append("\n");
            }
        }

        String message = sb.toString();
        if (message.length() > TELEGRAM_MAX_LENGTH) {
            message = message.substring(0, TELEGRAM_MAX_LENGTH - 3) + "...";
        }

        return message;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "요약 없음";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
