package com.curator.ainews.filter;

import com.curator.ainews.config.FilterConfig;
import com.curator.ainews.domain.Article;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedFilterTest {

    private RuleBasedFilter filter;
    private FilterConfig filterConfig;

    @BeforeEach
    void setUp() {
        filterConfig = new FilterConfig();
        filterConfig.setMaxAgeHours(48);
        filterConfig.setMinTitleLength(10);

        FilterConfig.Keywords keywords = new FilterConfig.Keywords();
        keywords.setAi(List.of("AI", "LLM", "GPT"));
        keywords.setBackend(List.of("API", "Spring", "서버"));
        keywords.setExclude(List.of("hiring", "채용"));
        filterConfig.setKeywords(keywords);

        filter = new RuleBasedFilter(filterConfig);
    }

    @Test
    void shouldPassArticleWithAiAndBackendKeywords() {
        Article article = Article.builder()
            .title("Spring AI로 LLM API 서버 구축하기")
            .url("https://example.com/1")
            .source("HACKER_NEWS")
            .publishedAt(LocalDateTime.now().minusHours(1))
            .build();

        List<Article> result = filter.filter(List.of(article));

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldPassArticleWithOnlyAiKeyword() {
        // 규칙 완화: AI 또는 백엔드 키워드 중 하나만 있어도 1차 필터를 통과하고, 품질 판단은 Gemini 평가 단계에 맡긴다
        Article article = Article.builder()
            .title("GPT-5 is coming soon to everyone")
            .url("https://example.com/2")
            .source("HACKER_NEWS")
            .publishedAt(LocalDateTime.now().minusHours(1))
            .build();

        List<Article> result = filter.filter(List.of(article));

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldRejectArticleWithoutAnyKeyword() {
        Article article = Article.builder()
            .title("Weekend recipes for a cozy autumn dinner")
            .url("https://example.com/no-keyword")
            .source("HACKER_NEWS")
            .publishedAt(LocalDateTime.now().minusHours(1))
            .build();

        List<Article> result = filter.filter(List.of(article));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldPassKoreanBlogWithAiKeywordOnly() {
        Article article = Article.builder()
            .title("네이버에서 LLM 활용한 검색 개선 사례")
            .url("https://d2.naver.com/1")
            .source("NAVER_D2")
            .publishedAt(LocalDateTime.now().minusHours(1))
            .build();

        List<Article> result = filter.filter(List.of(article));

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldRejectExcludeKeyword() {
        Article article = Article.builder()
            .title("AI Engineer hiring at Google Spring team")
            .url("https://example.com/3")
            .source("HACKER_NEWS")
            .publishedAt(LocalDateTime.now().minusHours(1))
            .build();

        List<Article> result = filter.filter(List.of(article));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldRejectExcludeKeywordKorean() {
        Article article = Article.builder()
            .title("AI Spring 개발자 채용 공고 안내")
            .url("https://example.com/4")
            .source("HACKER_NEWS")
            .publishedAt(LocalDateTime.now().minusHours(1))
            .build();

        List<Article> result = filter.filter(List.of(article));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldRejectTooOldArticle() {
        Article article = Article.builder()
            .title("Spring AI로 LLM API 서버 구축하기")
            .url("https://example.com/5")
            .source("HACKER_NEWS")
            .publishedAt(LocalDateTime.now().minusHours(100))
            .build();

        List<Article> result = filter.filter(List.of(article));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldPassArticleWithNullPublishedAt() {
        Article article = Article.builder()
            .title("Spring AI로 LLM API 서버 구축하기")
            .url("https://example.com/6")
            .source("HACKER_NEWS")
            .publishedAt(null)
            .build();

        List<Article> result = filter.filter(List.of(article));

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldRejectShortTitle() {
        Article article = Article.builder()
            .title("AI API")
            .url("https://example.com/7")
            .source("HACKER_NEWS")
            .publishedAt(LocalDateTime.now())
            .build();

        List<Article> result = filter.filter(List.of(article));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldMatchKeywordsInContent() {
        Article article = Article.builder()
            .title("새로운 기술 트렌드 분석 리포트")
            .url("https://example.com/8")
            .source("HACKER_NEWS")
            .content("이번 글에서는 AI와 Spring Boot API 서버를 다룹니다")
            .publishedAt(LocalDateTime.now())
            .build();

        List<Article> result = filter.filter(List.of(article));

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldFilterMultipleArticles() {
        Article pass = Article.builder()
            .title("GPT 기반 Spring API 서버 개발 가이드")
            .url("https://example.com/pass")
            .source("HACKER_NEWS")
            .publishedAt(LocalDateTime.now())
            .build();

        Article fail = Article.builder()
            .title("오늘의 날씨는 맑음입니다 좋은 하루")
            .url("https://example.com/fail")
            .source("HACKER_NEWS")
            .publishedAt(LocalDateTime.now())
            .build();

        List<Article> result = filter.filter(List.of(pass, fail));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUrl()).isEqualTo("https://example.com/pass");
    }

    @Test
    void shouldReturnEmptyForEmptyInput() {
        List<Article> result = filter.filter(List.of());
        assertThat(result).isEmpty();
    }
}
