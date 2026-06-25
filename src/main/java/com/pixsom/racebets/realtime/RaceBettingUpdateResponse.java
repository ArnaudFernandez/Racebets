package com.pixsom.racebets.realtime;

import java.time.Instant;
import java.util.List;

public record RaceBettingUpdateResponse(
        String raceId,
        Instant bettingOpenedAt,
        List<RaceRunnerResponse> runners
) {
}
