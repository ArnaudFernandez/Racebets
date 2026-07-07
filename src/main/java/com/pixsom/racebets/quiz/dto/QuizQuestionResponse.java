package com.pixsom.racebets.quiz.dto;

import java.util.List;

public record QuizQuestionResponse(
        Long id,
        int position,
        String text,
        String questionImageDataUrl,
        String answerImageDataUrl,
        int durationSeconds,
        List<QuizAnswerResponse> answers
) {
}
