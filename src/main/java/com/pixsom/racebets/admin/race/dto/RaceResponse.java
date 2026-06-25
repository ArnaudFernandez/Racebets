package com.pixsom.racebets.admin.race.dto;

import com.pixsom.racebets.enums.RaceState;

public record RaceResponse(
        Long id,
        String name,
        String raceImgUrl,
        RaceState state
) {
}
