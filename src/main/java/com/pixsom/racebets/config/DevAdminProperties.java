package com.pixsom.racebets.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "racebets.dev-admin")
public record DevAdminProperties(
        boolean enabled,
        String email,
        String accessCode
) {
}
