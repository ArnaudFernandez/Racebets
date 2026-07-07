package com.pixsom.racebets.quiz.dto;

import com.pixsom.racebets.quiz.QuizSetStatus;

import java.util.List;

public record QuizSetDetailResponse(
        Long id,
        String title,
        QuizSetStatus status,
        List<QuizQuestionResponse> questions
) {
}
