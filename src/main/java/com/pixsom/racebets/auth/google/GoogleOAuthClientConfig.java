package com.pixsom.racebets.auth.google;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(GoogleAuthProperties.class)
public class GoogleOAuthClientConfig {

    static final String AUTHORIZATION_BASE_URI = "/api/auth/google/authorize";
    static final String CALLBACK_BASE_URI = "/api/auth/google/callback/*";

    @Bean
    @ConditionalOnProperty(name = "racebets.google.enabled", havingValue = "true")
    ClientRegistrationRepository googleClientRegistrationRepository(GoogleAuthProperties properties) {
        if (properties.clientId() == null || properties.clientId().isBlank()
                || properties.clientSecret() == null || properties.clientSecret().isBlank()) {
            throw new IllegalStateException("GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET are required when Google authentication is enabled");
        }
        if (properties.loginCodeTtl() == null
                || properties.loginCodeTtl().compareTo(java.time.Duration.ofSeconds(30)) < 0
                || properties.loginCodeTtl().compareTo(java.time.Duration.ofMinutes(5)) > 0) {
            throw new IllegalStateException("GOOGLE_LOGIN_CODE_TTL must be between 30 seconds and 5 minutes");
        }

        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .clientId(properties.clientId())
                .clientSecret(properties.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/api/auth/google/callback/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri("https://accounts.google.com")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();

        return new InMemoryClientRegistrationRepository(google);
    }

    @Bean
    @ConditionalOnProperty(name = "racebets.google.enabled", havingValue = "true")
    OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository registrations) {
        return new InMemoryOAuth2AuthorizedClientService(registrations);
    }

    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "racebets.google.enabled", havingValue = "true")
    SecurityFilterChain googleSecurityFilterChain(HttpSecurity http,
                                                  ClientRegistrationRepository registrations,
                                                  GoogleOAuthSuccessHandler successHandler,
                                                  GoogleOAuthFailureHandler failureHandler) throws Exception {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(registrations, AUTHORIZATION_BASE_URI);
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());

        http
                .securityMatcher("/api/auth/google/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(endpoint -> endpoint
                                .baseUri(AUTHORIZATION_BASE_URI)
                                .authorizationRequestResolver(resolver))
                        .redirectionEndpoint(endpoint -> endpoint.baseUri(CALLBACK_BASE_URI))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler));

        return http.build();
    }
}
