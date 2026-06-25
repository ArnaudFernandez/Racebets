package com.pixsom.racebets.realtime;

import java.time.Instant;

public record RaceRunnerResponse(
        String runnerId,
        String runnerName,
        Instant updatedAt
) {
}
