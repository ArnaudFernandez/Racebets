package com.pixsom.racebets.admin.history.dto;

import java.time.Instant;

public record RaceHistoryWinnerResponse(
        int speedRank,
        Long userId,
        String userDisplayName,
        String userEmail,
        String horseName,
        Instant placedAt
) {
}
