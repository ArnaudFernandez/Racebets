package com.pixsom.racebets.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuizAnswerRequest(
        @NotBlank @Size(max = 240) String text,
        boolean correct
) {
}
