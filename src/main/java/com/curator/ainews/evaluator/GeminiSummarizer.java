package com.curator.ainews.evaluator;

import com.curator.ainews.domain.Article;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiSummarizer {

    private final GeminiClient geminiClient;

    public void summarize(List<Article> articles) {
        for (Article article : articles) {
            if (article.getSummary() != null && !article.getSummary().isEmpty()) continue;

            try {
                String prompt = String.format(
                    "아래 기술 기사를 백엔드 개발자 관점에서 한국어로 2~3줄로 요약해주세요.\n" +
                    "핵심 기술 포인트와 실무 적용 가능성을 강조해주세요.\n" +
                    "마크다운 없이 일반 텍스트로만 응답해주세요.\n\n" +
                    "제목: %s\n내용: %s\n출처: %s",
                    article.getTitle(),
                    article.getContent() != null
                        ? article.getContent().substring(0, Math.min(article.getContent().length(), 1000))
                        : "(내용 없음 - 제목 기반으로 요약해주세요)",
                    article.getSource()
                );

                String summary = geminiClient.call(prompt);
                if (summary != null) {
                    // Clean up any markdown formatting
                    summary = summary.replaceAll("```.*?```", "").trim();
                    // Truncate if too long
                    if (summary.length() > 500) {
                        summary = summary.substring(0, 497) + "...";
                    }
                    article.setSummary(summary);
                    log.info("Summarized: {}", article.getTitle());
                } else {
                    article.setSummary("요약을 생성할 수 없습니다.");
                    log.warn("Summary generation failed for: {}", article.getTitle());
                }
            } catch (Exception e) {
                log.error("Summarization error for [{}]: {}", article.getTitle(), e.getMessage());
                article.setSummary("요약을 생성할 수 없습니다.");
            }
        }
    }
}
