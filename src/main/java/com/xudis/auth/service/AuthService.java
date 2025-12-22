package com.xudis.auth.service;

import com.xudis.auth.dto.AuthResponse;
import com.xudis.auth.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    @Value("${auth.jwt.token-validity-seconds}")
    private long tokenValiditySeconds;

    public AuthResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            String accessToken = jwtTokenService.generateAccessToken(authentication);
            String refreshToken = jwtTokenService.generateRefreshToken(authentication.getName());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(tokenValiditySeconds)
                    .build();

        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {}", loginRequest.getUsername());
            throw e;
        }
    }

    public AuthResponse refresh(String refreshToken) {
        String username = jwtTokenService.getUsernameFromRefreshToken(refreshToken);
        if (username == null) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        // Create authentication for token generation
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username, null, null
        );

        String newAccessToken = jwtTokenService.generateAccessToken(authentication);
        String newRefreshToken = jwtTokenService.generateRefreshToken(username);

        // Revoke old refresh token
        jwtTokenService.revokeRefreshToken(refreshToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenValiditySeconds)
                .build();
    }

    public void logout(String token) {
        jwtTokenService.blacklistToken(token);
    }
}
