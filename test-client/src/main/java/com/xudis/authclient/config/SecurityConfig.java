package com.xudis.authclient.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository
    ) throws Exception {

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/test/**", "/css/**", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        // 关键：对 Authorization Request 强制加 PKCE
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(
                                        pkceAuthorizationRequestResolver(clientRegistrationRepository)
                                )
                        )
                        .loginPage("/oauth2/authorization/xudis-auth")
                        .defaultSuccessUrl("/oauth2/success", true)
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        return http.build();
    }

    private OAuth2AuthorizationRequestResolver pkceAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository
    ) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        "/oauth2/authorization"
                );

        // Spring Security 内置的 PKCE 自定义器：会自动生成 code_verifier 并构造 code_challenge/S256
        resolver.setAuthorizationRequestCustomizer(
                OAuth2AuthorizationRequestCustomizers.withPkce()
        );

        return resolver;
    }
}