package com.pixsom.racebets.quiz.dto;

import jakarta.validation.constraints.NotNull;

public record QuizAnswerSubmitRequest(@NotNull Long answerId) {
}
