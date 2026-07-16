package com.pixsom.racebets.admin.race.dto;

import com.pixsom.racebets.enums.RaceState;
import jakarta.validation.constraints.NotNull;

public record RaceStateRequest(@NotNull RaceState state) {
}
