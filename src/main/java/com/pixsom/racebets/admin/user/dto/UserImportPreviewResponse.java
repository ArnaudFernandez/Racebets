package com.pixsom.racebets.admin.user.dto;

import java.util.List;

public record UserImportPreviewResponse(
        String fileDigest,
        String planFingerprint,
        int totalRows,
        int createCount,
        int updateCount,
        int unchangedCount,
        int errorCount,
        boolean importable,
        List<UserImportRowResponse> rows
) {
}
