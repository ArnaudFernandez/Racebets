package com.pixsom.racebets.admin.raceentry.dto;

public record RaceEntryResponse(
        Long id,
        Long raceId,
        String raceName,
        Long horseId,
        String horseName,
        int horseNumber,
        Integer rank
) {
}
