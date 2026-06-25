package com.pixsom.racebets.realtime;

import com.pixsom.racebets.entities.RaceEntry;
import com.pixsom.racebets.repositories.RaceEntryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RealtimeRaceBettingService {

    private static final String DEFAULT_LIVE_RACE_ID = "paris-fictifs-main";

    private final RaceEntryRepository raceEntryRepository;

    public RealtimeRaceBettingService(RaceEntryRepository raceEntryRepository) {
        this.raceEntryRepository = raceEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<RaceBettingUpdateResponse> currentSnapshots() {
        Instant now = Instant.now();
        List<RaceEntry> entries = raceEntryRepository.findAllBy(
                Sort.by(Sort.Order.asc("race.name"), Sort.Order.asc("horseNumber"))
        );

        if (entries.isEmpty()) {
            return List.of(new RaceBettingUpdateResponse(DEFAULT_LIVE_RACE_ID, now, List.of()));
        }

        Map<Long, List<RaceEntry>> entriesByRace = new LinkedHashMap<>();
        for (RaceEntry entry : entries) {
            entriesByRace.computeIfAbsent(entry.getRace().getId(), ignored -> new java.util.ArrayList<>()).add(entry);
        }

        return entriesByRace.entrySet().stream()
                .map(group -> new RaceBettingUpdateResponse(
                        group.getKey().toString(),
                        now,
                        group.getValue().stream()
                                .map(entry -> new RaceRunnerResponse(
                                        entry.getId().toString(),
                                        "#" + entry.getHorseNumber() + " " + entry.getHorse().getName(),
                                        now
                                ))
                                .toList()
                ))
                .toList();
    }
}
