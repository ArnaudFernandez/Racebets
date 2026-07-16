package com.pixsom.racebets.admin.history.dto;

public record RaceHistoryResultResponse(
        int rank,
        int horseNumber,
        String horseName,
        long voteCount
) {
}
