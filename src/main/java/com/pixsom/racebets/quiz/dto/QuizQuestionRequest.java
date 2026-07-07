package com.pixsom.racebets.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuizQuestionRequest(
        @NotBlank @Size(max = 500) String text,
        String questionImageDataUrl,
        String answerImageDataUrl,
        @Min(5) @Max(300) Integer durationSeconds,
        @NotEmpty @Size(min = 2, max = 4) List<@Valid QuizAnswerRequest> answers
) {
}
