package com.pixsom.racebets.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 80) String surname,
        @Email @NotBlank @Size(max = 180) String email,
        @NotBlank @Size(min = 8, max = 128) String accessCode,
        @Past LocalDate birthDate
) {
}
