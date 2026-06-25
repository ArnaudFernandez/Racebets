package com.pixsom.racebets.admin.raceentry.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RaceEntryRequest(
        @NotNull
        Long raceId,

        @NotNull
        Long horseId,

        @NotNull
        @Positive
        Integer horseNumber,

        @Positive
        Integer rank
) {
}
