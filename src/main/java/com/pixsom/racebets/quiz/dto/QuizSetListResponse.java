package com.pixsom.racebets.quiz.dto;

import com.pixsom.racebets.quiz.QuizSetStatus;

public record QuizSetListResponse(
        Long id,
        String title,
        QuizSetStatus status,
        int questionCount
) {
}
