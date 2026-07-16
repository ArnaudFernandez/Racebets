package com.pixsom.racebets.admin.race.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RaceRunnersRequest(
        @NotNull List<@NotNull Long> horseIds
) {
}
