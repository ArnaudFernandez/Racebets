package com.pixsom.racebets.admin.race.dto;

import com.pixsom.racebets.enums.RaceState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RaceRequest(
        @NotBlank
        @Size(max = 120)
        String name,

        @Size(max = 500)
        String raceImgUrl,

        RaceState state
) {
}
