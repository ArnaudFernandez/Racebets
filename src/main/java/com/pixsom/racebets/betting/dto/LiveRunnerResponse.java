package com.pixsom.racebets.betting.dto;

public record LiveRunnerResponse(
        Long entryId,
        Long horseId,
        String horseName,
        int horseNumber,
        Integer rank,
        long betCount
) {
}
