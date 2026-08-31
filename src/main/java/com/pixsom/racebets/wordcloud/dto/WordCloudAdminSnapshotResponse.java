package com.pixsom.racebets.wordcloud.dto;

import com.pixsom.racebets.wordcloud.WordCloudQuestionStatus;

import java.time.Instant;
import java.util.List;

public record WordCloudAdminSnapshotResponse(
        Long id,
        String text,
        WordCloudQuestionStatus status,
        Instant createdAt,
        Instant openedAt,
        Instant revealedAt,
        Instant closedAt,
        long submissionCount,
        List<WordCloudAdminWordResponse> responses
) {
}
