package com.pixsom.racebets.admin.user.dto;

public record UserImportRowResponse(
        long lineNumber,
        String email,
        String name,
        String surname,
        UserImportAction action,
        String warning,
        String error
) {
}
