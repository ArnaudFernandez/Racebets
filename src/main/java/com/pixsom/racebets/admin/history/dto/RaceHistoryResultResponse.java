package com.pixsom.racebets.admin.history.dto;

public record RaceHistoryResultResponse(
        Long entryId,
        int rank,
        int horseNumber,
        String horseName,
        long voteCount
) {
}
