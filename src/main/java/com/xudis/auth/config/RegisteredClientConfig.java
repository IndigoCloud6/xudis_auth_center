package com.xudis.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RegisteredClientConfig {

    private final RegisteredClientRepository registeredClientRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner initializeRegisteredClients() {
        return args -> {
            // Check if demo-client already exists
            RegisteredClient existingClient = registeredClientRepository.findByClientId("demo-client");
            if (existingClient == null) {
                log.info("Initializing default OAuth2 client: demo-client");
                
                RegisteredClient demoClient = RegisteredClient.withId(UUID.randomUUID().toString())
                        .clientId("demo-client")
                        .clientSecret(passwordEncoder.encode("demo-secret"))
                        .clientName("Demo Client")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .redirectUri("http://127.0.0.1:8080/login/oauth2/code/demo-client")
                        .redirectUri("http://localhost:8080/login/oauth2/code/demo-client")
                        .redirectUri("http://localhost:8080/login/oauth2/code/xudis-auth")
                        .redirectUri("http://127.0.0.1:8080/login/oauth2/code/xudis-auth")
                        .redirectUri("http://127.0.0.1:8080/authorized")
                        .redirectUri("http://localhost:8080/authorized")
                        .postLogoutRedirectUri("http://127.0.0.1:8080/logged-out")
                        .postLogoutRedirectUri("http://localhost:8080/logged-out")
                        .scope(OidcScopes.OPENID)
                        .scope(OidcScopes.PROFILE)
                        .scope(OidcScopes.EMAIL)
                        .scope("read")
                        .scope("write")
                        .clientSettings(ClientSettings.builder()
                                .requireAuthorizationConsent(true)
                                .requireProofKey(true)
                                .build())
                        .tokenSettings(TokenSettings.builder()
                                .accessTokenTimeToLive(Duration.ofHours(1))
                                .refreshTokenTimeToLive(Duration.ofDays(1))
                                .reuseRefreshTokens(false)
                                .build())
                        .build();

                registeredClientRepository.save(demoClient);
                log.info("Default OAuth2 client 'demo-client' initialized successfully");
            } else {
                log.info("OAuth2 client 'demo-client' already exists, skipping initialization");
            }
        };
    }
}
