package com.pixsom.racebets.admin.history.dto;

import java.time.Instant;
import java.util.List;

public record RaceHistoryDetailResponse(
        Long raceId,
        String raceName,
        Instant finishedAt,
        long totalVotes,
        List<RaceHistoryResultResponse> result,
        List<RaceHistoryWinnerResponse> winners,
        List<RaceHistoryVoteResponse> votes
) {
}
