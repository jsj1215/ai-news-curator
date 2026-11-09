package com.curator.ainews.domain;

import com.curator.ainews.auth.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
        userRepository.deleteAll();

        // Create user and get token
        Map<String, String> signup = Map.of(
            "name", "테스트",
            "email", "article-test@example.com",
            "password", "password123"
        );
        try {
            mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signup)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        accessToken = jwtTokenProvider.generateAccessToken("article-test@example.com");
    }

    @Test
    void shouldRequireAuthForArticles() throws Exception {
        mockMvc.perform(get("/api/articles/today"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnTodayArticles() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        articleRepository.save(Article.builder()
            .title("Today Article")
            .url("https://example.com/today")
            .source("HN")
            .totalScore(8.0)
            .sent(true)
            .sentAt(now)
            .build());

        mockMvc.perform(get("/api/articles/today")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].articles[0].title").value("Today Article"));
    }

    @Test
    void shouldReturnEmptyForNoArticlesToday() throws Exception {
        mockMvc.perform(get("/api/articles/today")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnHistoryByDate() throws Exception {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1).withHour(12);
        articleRepository.save(Article.builder()
            .title("Yesterday Article")
            .url("https://example.com/yesterday")
            .source("HN")
            .totalScore(7.0)
            .sent(true)
            .sentAt(yesterday)
            .build());

        String dateParam = LocalDate.now().minusDays(1).toString();
        mockMvc.perform(get("/api/articles/history")
                .param("date", dateParam)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].articles[0].title").value("Yesterday Article"));
    }

    @Test
    void shouldReturnArticleById() throws Exception {
        Article saved = articleRepository.save(Article.builder()
            .title("Detail Article")
            .url("https://example.com/detail")
            .source("HN")
            .totalScore(9.0)
            .summary("This is a summary")
            .build());

        mockMvc.perform(get("/api/articles/" + saved.getId())
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Detail Article"))
            .andExpect(jsonPath("$.summary").value("This is a summary"));
    }

    @Test
    void shouldReturn404ForNonExistentArticle() throws Exception {
        mockMvc.perform(get("/api/articles/99999")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnDistinctSentDates() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        articleRepository.save(Article.builder()
            .title("A1").url("https://example.com/d1").source("HN")
            .sent(true).sentAt(now).build());
        articleRepository.save(Article.builder()
            .title("A2").url("https://example.com/d2").source("HN")
            .sent(true).sentAt(now.minusDays(1)).build());

        mockMvc.perform(get("/api/articles/dates")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
    }
}
