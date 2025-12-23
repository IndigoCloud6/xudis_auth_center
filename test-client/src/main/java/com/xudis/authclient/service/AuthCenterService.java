package com.xudis.authclient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xudis.authclient.dto.LoginRequest;
import com.xudis.authclient.dto.LoginResponse;
import com.xudis.authclient.dto.RefreshRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@Slf4j
public class AuthCenterService {

    private final WebClient webClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public AuthCenterService(WebClient.Builder webClientBuilder,
                            @Value("${auth.center.base-url}") String baseUrl,
                            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    /**
     * Test custom login API
     */
    public LoginResponse login(String username, String password) {
        log.info("Testing login with username: {}", username);
        
        LoginRequest request = new LoginRequest(username, password);
        
        try {
            LoginResponse response = webClient.post()
                    .uri("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LoginResponse.class)
                    .block();
            
            log.info("Login successful");
            return response;
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test refresh token API
     */
    public LoginResponse refresh(String refreshToken) {
        log.info("Testing token refresh");
        
        RefreshRequest request = new RefreshRequest(refreshToken);
        
        try {
            LoginResponse response = webClient.post()
                    .uri("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LoginResponse.class)
                    .block();
            
            log.info("Token refresh successful");
            return response;
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new RuntimeException("Token refresh failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test logout API
     */
    public String logout(String accessToken) {
        log.info("Testing logout");
        
        try {
            String response = webClient.post()
                    .uri("/api/auth/logout")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.info("Logout successful");
            return response;
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            throw new RuntimeException("Logout failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test OIDC Discovery endpoint
     */
    public Map<String, Object> getOidcConfiguration() {
        log.info("Testing OIDC Discovery endpoint");
        
        try {
            Map<String, Object> config = webClient.get()
                    .uri("/.well-known/openid-configuration")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            log.info("OIDC Discovery successful");
            return config;
        } catch (Exception e) {
            log.error("OIDC Discovery failed: {}", e.getMessage());
            throw new RuntimeException("OIDC Discovery failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test JWKS endpoint
     */
    public Map<String, Object> getJwks() {
        log.info("Testing JWKS endpoint");
        
        try {
            Map<String, Object> jwks = webClient.get()
                    .uri("/oauth2/jwks")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            log.info("JWKS retrieval successful");
            return jwks;
        } catch (Exception e) {
            log.error("JWKS retrieval failed: {}", e.getMessage());
            throw new RuntimeException("JWKS retrieval failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test OAuth2 Client Credentials flow
     */
    public Map<String, Object> getClientCredentialsToken(String clientId, String clientSecret, String scope) {
        log.info("Testing OAuth2 Client Credentials flow");
        
        try {
            Map<String, Object> response = webClient.post()
                    .uri("/oauth2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                    .bodyValue("grant_type=client_credentials&scope=" + scope)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            log.info("Client Credentials token obtained successfully");
            return response;
        } catch (Exception e) {
            log.error("Client Credentials flow failed: {}", e.getMessage());
            throw new RuntimeException("Client Credentials flow failed: " + e.getMessage(), e);
        }
    }

    /**
     * Parse JWT to extract claims
     */
    public Map<String, Object> parseJwt(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT format");
            }
            
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            return objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse JWT: {}", e.getMessage());
            throw new RuntimeException("Failed to parse JWT: " + e.getMessage(), e);
        }
    }
}
