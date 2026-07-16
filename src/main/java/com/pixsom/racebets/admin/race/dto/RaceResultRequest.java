package com.pixsom.racebets.admin.race.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RaceResultRequest(@NotEmpty List<@NotNull Long> orderedEntryIds) {
}
