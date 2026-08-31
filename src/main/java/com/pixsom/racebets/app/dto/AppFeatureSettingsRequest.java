package com.pixsom.racebets.app.dto;

import com.pixsom.racebets.app.AppMode;
import jakarta.validation.constraints.NotNull;

public record AppFeatureSettingsRequest(@NotNull AppMode activeMode) {
}
