package com.pixsom.racebets.wordcloud.dto;

import com.pixsom.racebets.wordcloud.WordCloudQuestionStatus;

import java.time.Instant;

public record WordCloudQuestionListResponse(
        Long id,
        String text,
        WordCloudQuestionStatus status,
        long submissionCount,
        Instant createdAt,
        Instant openedAt,
        Instant revealedAt,
        Instant closedAt
) {
}
