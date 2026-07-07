package com.pixsom.racebets.admin.user.dto;

import com.pixsom.racebets.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record AdminUserRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 80) String surname,
        @Email @NotBlank @Size(max = 180) String email,
        @Size(min = 8, max = 128) String accessCode,
        @Past LocalDate birthDate,
        boolean present,
        @NotEmpty Set<Role> roles
) {
}
