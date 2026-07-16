package com.pixsom.racebets.betting.dto;

import com.pixsom.racebets.enums.BetState;

import java.time.Instant;

public record UserBetResponse(
        Long entryId,
        String horseName,
        Instant placedAt,
        BetState state,
        Integer speedRank
) {
}
