package com.pixsom.racebets.quiz.dto;

public record QuizAnswerResponse(
        Long id,
        int position,
        String text,
        Boolean correct
) {
}
