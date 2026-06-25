package com.pixsom.racebets.auth.dto;

import com.pixsom.racebets.enums.Role;

import java.util.Set;

public record LoginResponse (String token, String tokenType, long expiresIn, Set<Role> roles) {
}
