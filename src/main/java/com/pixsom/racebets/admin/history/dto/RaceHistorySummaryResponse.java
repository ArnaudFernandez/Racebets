package com.pixsom.racebets.admin.history.dto;

import java.time.Instant;

public record RaceHistorySummaryResponse(
        Long raceId,
        String raceName,
        Instant finishedAt,
        int runnerCount,
        long totalVotes,
        long winnerCount,
        String winningHorseName
) {
}
