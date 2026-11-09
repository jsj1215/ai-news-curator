package com.curator.ainews.config;

import com.curator.ainews.auth.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.UUID;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // H2 콘솔은 DispatcherServlet 밖의 별도 서블릿이라 MVC 기준 문자열 매처로는 잡히지 않는다.
    // 서블릿과 무관하게 요청 URI로 매칭하도록 AntPathRequestMatcher를 명시한다.
    private static final RequestMatcher H2_CONSOLE = new AntPathRequestMatcher("/h2-console/**");
    private static final RequestMatcher ADMIN_PATHS = new OrRequestMatcher(
        new AntPathRequestMatcher("/admin/**"),
        new AntPathRequestMatcher("/api/pipeline/**"),
        H2_CONSOLE
    );

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Environment environment;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:}")
    private String adminPassword;

    /**
     * 관리 경로 전용 체인. 파이프라인 수동 실행은 Gemini 쿼터를 소모하므로 공개하지 않는다.
     * 브라우저에서 바로 쓰는 Thymeleaf 화면이라 별도 로그인 화면 없이 HTTP Basic을 쓰고,
     * 브라우저가 자격 증명을 자동 첨부하는 방식이므로 CSRF 보호를 함께 켠다.
     * 관리자 계정은 이 체인에만 연결해 /api/auth 의 JWT 회원 인증과 섞이지 않게 한다.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(ADMIN_PATHS)
            .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
            .httpBasic(basic -> basic.realmName("AI News Curator Admin"))
            .authenticationManager(adminAuthenticationManager())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf
                // 템플릿의 <meta name="_csrf">로 토큰을 내려주고 app.js가 X-XSRF-TOKEN 헤더로 돌려보낸다
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers(H2_CONSOLE))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    private AuthenticationManager adminAuthenticationManager() {
        String password = adminPassword;
        if (password == null || password.isBlank()) {
            // 운영에서는 임시 비밀번호를 만들지 않는다. 로그 파일에 관리자 비밀번호가 남기 때문
            if (environment.acceptsProfiles(Profiles.of("prod"))) {
                throw new IllegalStateException("prod 프로필에서는 ADMIN_PASSWORD 환경변수가 필요합니다");
            }
            password = UUID.randomUUID().toString();
            log.warn("ADMIN_PASSWORD가 설정되지 않아 임시 관리자 비밀번호를 생성했습니다 (재시작 시 변경됨). username={}, password={}",
                adminUsername, password);
        }
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
            User.withUsername(adminUsername)
                .password(passwordEncoder().encode(password))
                .roles("ADMIN")
                .build());
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder());
        provider.setUserDetailsService(users);
        return new ProviderManager(provider);
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/", "/history", "/articles/**", "/error/**").permitAll()
                .requestMatchers("/api/articles/**").authenticated()
                .anyRequest().permitAll()
            )
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
