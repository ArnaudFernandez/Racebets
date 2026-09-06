package com.pixsom.racebets.quiz.dto;

import com.pixsom.racebets.quiz.QuizSessionPhase;

public record QuizSessionSummaryResponse(
        Long id,
        Long quizSetId,
        String title,
        QuizSessionPhase phase,
        int currentQuestionIndex,
        int questionCount,
        long participantCount
) {
}
