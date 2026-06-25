package com.pixsom.racebets.admin.horse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HorseRequest(
        @NotBlank
        @Size(max = 100)
        String name
) {
}
