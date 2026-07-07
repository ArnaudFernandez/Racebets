package com.pixsom.racebets.admin.user.dto;

import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.enums.Role;

import java.time.LocalDate;
import java.util.Set;

public record AdminUserResponse(
        Long id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        boolean present,
        Set<Role> roles
) {
    public static AdminUserResponse from(AppUser user) {
        return new AdminUserResponse(
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
