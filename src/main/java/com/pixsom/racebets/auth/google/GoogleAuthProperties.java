package com.pixsom.racebets.auth.google;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("racebets.google")
public record GoogleAuthProperties(
        boolean enabled,
        String clientId,
        String clientSecret,
        Duration loginCodeTtl
) {
}
