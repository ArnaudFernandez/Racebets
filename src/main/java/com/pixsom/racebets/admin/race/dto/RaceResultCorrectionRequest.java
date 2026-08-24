package com.pixsom.racebets.admin.race.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RaceResultCorrectionRequest(
        @NotEmpty List<@NotNull Long> expectedOrderedEntryIds,
        @NotEmpty List<@NotNull Long> orderedEntryIds
) {
}
