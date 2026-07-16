package com.pixsom.racebets.admin.race.dto;

import com.pixsom.racebets.enums.RaceState;

import java.time.Instant;
import java.util.List;

public record RaceControlResponse(
        Long id,
        String name,
        String raceImgUrl,
        RaceState state,
        boolean visibleOnLive,
        List<RaceControlEntryResponse> entries,
        long totalBets,
        Instant updatedAt
) {
}
