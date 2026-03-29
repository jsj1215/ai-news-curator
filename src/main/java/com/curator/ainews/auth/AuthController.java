package com.curator.ainews.auth;

import com.curator.ainews.auth.dto.*;
import com.curator.ainews.domain.User;
import com.curator.ainews.domain.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "이미 사용 중인 이메일입니다"));
        }

        User user = User.builder()
            .name(request.getName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .build();

        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new TokenResponse(accessToken, refreshToken, user.getName(), user.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
            .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
            .map(user -> {
                String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
                String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
                return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken, user.getName(), user.getEmail()));
            })
            .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "유효하지 않은 refresh token입니다"));
        }

        String email = jwtTokenProvider.getEmailFromToken(request.getRefreshToken());
        return userRepository.findByEmail(email)
            .map(user -> {
                String accessToken = jwtTokenProvider.generateAccessToken(email);
                String refreshToken = jwtTokenProvider.generateRefreshToken(email);
                return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken, user.getName(), user.getEmail()));
            })
            .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null));
    }
}
