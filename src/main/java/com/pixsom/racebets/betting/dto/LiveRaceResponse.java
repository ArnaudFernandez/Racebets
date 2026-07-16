package com.pixsom.racebets.betting.dto;

import com.pixsom.racebets.enums.RaceState;

import java.time.Instant;
import java.util.List;

public record LiveRaceResponse(
        Long raceId,
        String raceName,
        String raceImgUrl,
        RaceState state,
        List<LiveRunnerResponse> runners,
        UserBetResponse userBet,
        long totalBets,
        Instant updatedAt
) {
}
