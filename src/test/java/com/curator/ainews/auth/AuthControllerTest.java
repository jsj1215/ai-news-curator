package com.curator.ainews.auth;

import com.curator.ainews.domain.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldSignupSuccessfully() throws Exception {
        Map<String, String> request = Map.of(
            "name", "테스트유저",
            "email", "test@example.com",
            "password", "password123"
        );

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.name").value("테스트유저"))
            .andExpect(jsonPath("$.email").value("test@example.com"));

        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        Map<String, String> request = Map.of(
            "name", "유저1",
            "email", "dup@example.com",
            "password", "password123"
        );

        // First signup
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // Duplicate signup
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("이미 사용 중인 이메일입니다"));
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        Map<String, String> request = Map.of(
            "name", "유저",
            "email", "not-an-email",
            "password", "password123"
        );

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectShortPassword() throws Exception {
        Map<String, String> request = Map.of(
            "name", "유저",
            "email", "test2@example.com",
            "password", "short"
        );

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        // Signup first
        Map<String, String> signup = Map.of(
            "name", "로그인유저",
            "email", "login@example.com",
            "password", "password123"
        );
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signup)))
            .andExpect(status().isCreated());

        // Login
        Map<String, String> login = Map.of(
            "email", "login@example.com",
            "password", "password123"
        );
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.name").value("로그인유저"));
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {
        Map<String, String> signup = Map.of(
            "name", "유저",
            "email", "wrong@example.com",
            "password", "password123"
        );
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signup)))
            .andExpect(status().isCreated());

        Map<String, String> login = Map.of(
            "email", "wrong@example.com",
            "password", "wrongpassword"
        );
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectNonExistentUser() throws Exception {
        Map<String, String> login = Map.of(
            "email", "noone@example.com",
            "password", "password123"
        );
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRefreshToken() throws Exception {
        // Signup to get tokens
        Map<String, String> signup = Map.of(
            "name", "리프레시유저",
            "email", "refresh@example.com",
            "password", "password123"
        );
        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signup)))
            .andExpect(status().isCreated())
            .andReturn();

        String responseBody = signupResult.getResponse().getContentAsString();
        Map<String, String> tokenResponse = objectMapper.readValue(responseBody, Map.class);
        String refreshToken = tokenResponse.get("refreshToken");

        // Refresh
        Map<String, String> refreshRequest = Map.of("refreshToken", refreshToken);
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void shouldRejectInvalidRefreshToken() throws Exception {
        Map<String, String> request = Map.of("refreshToken", "invalid.token.here");

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }
}
