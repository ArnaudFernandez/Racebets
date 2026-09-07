package com.pixsom.racebets.quiz.dto;

import com.pixsom.racebets.quiz.QuizSessionPhase;

import java.time.Instant;
import java.util.List;

public record QuizSessionSnapshotResponse(
        Long id,
        Long quizSetId,
        String title,
        QuizSessionPhase phase,
        int currentQuestionIndex,
        int questionCount,
        long participantCount,
        Instant serverTime,
        Instant phaseStartedAt,
        Instant questionEndsAt,
        boolean joined,
        Long selectedAnswerId,
        Long correctAnswerId,
        long submittedAnswers,
        QuizLiveQuestionResponse currentQuestion,
        List<QuizScoreResponse> scores
) {
}
