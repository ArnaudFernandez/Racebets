package com.pixsom.racebets.wordcloud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WordCloudResponseRequest(@NotBlank @Size(max = 80) String text) {
}
