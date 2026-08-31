package com.pixsom.racebets.wordcloud.dto;

import com.pixsom.racebets.wordcloud.WordCloudQuestionStatus;

import java.time.Instant;
import java.util.List;

public record WordCloudSnapshotResponse(
        Long id,
        String text,
        WordCloudQuestionStatus status,
        Instant openedAt,
        long submissionCount,
        String currentUserResponse,
        List<WordCloudWordResponse> words
) {
}
