package com.pixsom.racebets.betting.dto;

import jakarta.validation.constraints.NotNull;

public record PlaceBetRequest(@NotNull Long raceEntryId) {
}
