package com.pixsom.racebets.wordcloud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WordCloudQuestionRequest(@NotBlank @Size(max = 300) String text) {
}
