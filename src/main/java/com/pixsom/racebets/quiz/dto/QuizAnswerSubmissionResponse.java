package com.pixsom.racebets.quiz.dto;

import com.pixsom.racebets.quiz.QuizSessionPhase;

import java.time.Instant;

public record QuizAnswerSubmissionResponse(
        Long selectedAnswerId,
        QuizSessionPhase phase,
        Instant serverTime
) {
}
