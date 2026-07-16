package com.pixsom.racebets.betting.dto;

import com.pixsom.racebets.enums.BetState;

import java.time.Instant;

public record BetHistoryResponse(
        Long raceId,
        String raceName,
        Instant finishedAt,
        String selectedHorseName,
        String winningHorseName,
        Instant placedAt,
        BetState state,
        Integer speedRank
) {
}
