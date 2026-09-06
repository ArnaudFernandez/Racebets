package com.pixsom.racebets.admin.user.dto;

public record UserImportResultResponse(
        int createdCount,
        int updatedCount,
        int unchangedCount
) {
}
