package com.pixsom.racebets.quiz.dto;

import java.util.List;

public record QuizLiveQuestionResponse(
        Long id,
        int position,
        String text,
        String questionImageUrl,
        String answerImageUrl,
        int durationSeconds,
        List<QuizAnswerResponse> answers
) {
}
