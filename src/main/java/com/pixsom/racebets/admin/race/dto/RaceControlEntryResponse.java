package com.pixsom.racebets.admin.race.dto;

public record RaceControlEntryResponse(
        Long entryId,
        Long horseId,
        String horseName,
        int horseNumber,
        Integer rank,
        long betCount
) {
}
