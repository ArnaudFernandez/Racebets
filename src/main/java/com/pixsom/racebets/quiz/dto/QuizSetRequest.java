package com.pixsom.racebets.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuizSetRequest(
        @NotBlank @Size(max = 140) String title,
        @NotEmpty List<@Valid QuizQuestionRequest> questions
) {
}
