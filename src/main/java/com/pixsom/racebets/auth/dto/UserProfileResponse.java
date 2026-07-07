package com.pixsom.racebets.auth.dto;

import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.enums.Role;

import java.time.LocalDate;
import java.util.Set;

public record UserProfileResponse(
        Long id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        boolean present,
        Set<Role> roles
) {
    public static UserProfileResponse from(AppUser user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getSurname(),
                user.getBirthDate(),
                user.getEmail(),
                user.isPresent(),
                user.getRoles()
        );
    }
}
