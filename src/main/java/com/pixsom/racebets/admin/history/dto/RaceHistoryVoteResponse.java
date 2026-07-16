package com.pixsom.racebets.admin.history.dto;

import com.pixsom.racebets.enums.BetState;

import java.time.Instant;

public record RaceHistoryVoteResponse(
        Long betId,
        Long userId,
        String userDisplayName,
        String userEmail,
        String horseName,
        Instant placedAt,
        BetState state
) {
}
