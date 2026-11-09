package com.curator.ainews.pipeline;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PipelineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NewsPipeline newsPipeline;

    // src/test/resources/application.yml 의 admin 계정
    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "test-admin-password";

    @Test
    void shouldRejectRunWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/pipeline/run").with(csrf()))
            .andExpect(status().isUnauthorized());

        verify(newsPipeline, never()).execute();
    }

    @Test
    void shouldRejectRunWithWrongPassword() throws Exception {
        mockMvc.perform(post("/api/pipeline/run").with(httpBasic(ADMIN, "wrong")).with(csrf()))
            .andExpect(status().isUnauthorized());

        verify(newsPipeline, never()).execute();
    }

    @Test
    void shouldRejectRunWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/pipeline/run").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
            .andExpect(status().isForbidden());

        verify(newsPipeline, never()).execute();
    }

    @Test
    void shouldMatchH2ConsolePathInAdminChain() throws Exception {
        // 테스트 프로필에선 H2 콘솔 서블릿이 꺼져 있지만, 보안 체인이 경로를 잡는지는 확인할 수 있다
        mockMvc.perform(get("/h2-console/"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAdminPageWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/admin/pipeline"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRunPipelineManually() throws Exception {
        PipelineResult result = PipelineResult.success(10, 5, 5, 3);
        when(newsPipeline.execute()).thenReturn(result);

        mockMvc.perform(post("/api/pipeline/run").with(httpBasic(ADMIN, ADMIN_PASSWORD)).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.collected").value(10))
            .andExpect(jsonPath("$.sent").value(3));
    }

    @Test
    void shouldReturnSkippedWhenAlreadyRunning() throws Exception {
        PipelineResult result = PipelineResult.skipped();
        when(newsPipeline.execute()).thenReturn(result);

        mockMvc.perform(post("/api/pipeline/run").with(httpBasic(ADMIN, ADMIN_PASSWORD)).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SKIPPED"));
    }

    @Test
    void shouldReturnPipelineStatus() throws Exception {
        when(newsPipeline.isRunning()).thenReturn(false);

        mockMvc.perform(get("/api/pipeline/status").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.running").value(false));
    }

    @Test
    void shouldReturnRunningStatus() throws Exception {
        when(newsPipeline.isRunning()).thenReturn(true);

        mockMvc.perform(get("/api/pipeline/status").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.running").value(true));
    }

    @Test
    void shouldReturnFailureResult() throws Exception {
        PipelineResult result = PipelineResult.failure("API connection failed");
        when(newsPipeline.execute()).thenReturn(result);

        mockMvc.perform(post("/api/pipeline/run").with(httpBasic(ADMIN, ADMIN_PASSWORD)).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FAILURE"));
    }
}
