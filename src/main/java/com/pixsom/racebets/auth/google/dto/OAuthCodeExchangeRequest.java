package com.pixsom.racebets.auth.google.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OAuthCodeExchangeRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_-]{43}$")
        String code,
        @Size(max = 128)
        String accessCode
) {
}
