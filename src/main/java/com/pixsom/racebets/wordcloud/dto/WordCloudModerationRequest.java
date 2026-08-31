package com.pixsom.racebets.wordcloud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WordCloudModerationRequest(
        @NotBlank @Size(max = 80) String text
) {
}
