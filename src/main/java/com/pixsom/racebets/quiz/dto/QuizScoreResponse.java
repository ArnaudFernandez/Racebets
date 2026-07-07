package com.pixsom.racebets.quiz.dto;

public record QuizScoreResponse(
        Long userId,
        String displayName,
        int score
) {
}
