package com.pixsom.racebets.admin.race;

import com.pixsom.racebets.admin.ConflictException;
import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.admin.race.dto.RaceControlEntryResponse;
import com.pixsom.racebets.admin.race.dto.RaceControlResponse;
import com.pixsom.racebets.entities.Bet;
import com.pixsom.racebets.entities.Horse;
import com.pixsom.racebets.entities.Race;
import com.pixsom.racebets.entities.RaceEntry;
import com.pixsom.racebets.enums.RaceState;
import com.pixsom.racebets.repositories.BetRepository;
import com.pixsom.racebets.repositories.HorseRepository;
import com.pixsom.racebets.repositories.RaceEntryRepository;
import com.pixsom.racebets.repositories.RaceRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.LinkedHashSet;

@Service
public class RaceWorkflowService {

    private static final Set<RaceState> ACTIVE_STATES = EnumSet.of(
            RaceState.STANDBY, RaceState.BET_STARTING, RaceState.BETTING, RaceState.BET_CLOSED);

    private final RaceRepository raceRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final BetRepository betRepository;
    private final HorseRepository horseRepository;

    public RaceWorkflowService(RaceRepository raceRepository, RaceEntryRepository raceEntryRepository,
                               BetRepository betRepository, HorseRepository horseRepository) {
        this.raceRepository = raceRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.betRepository = betRepository;
        this.horseRepository = horseRepository;
    }

    @Transactional(readOnly = true)
    public RaceControlResponse control(Long raceId) {
        return snapshot(findRace(raceId));
    }

    @Transactional
    public RaceControlResponse transition(Long raceId, RaceState targetState) {
        Race race = findLockedRace(raceId);
        RaceState expected = nextState(race.getState());
        if (expected == null || expected != targetState || targetState == RaceState.FINISHED) {
            throw new ConflictException("Invalid race state transition");
        }

        if (targetState == RaceState.STANDBY) {
            if (raceRepository.existsByStateInAndIdNot(ACTIVE_STATES, raceId)) {
                throw new ConflictException("Another race is already active");
            }
            if (raceEntryRepository.findAllByRace_Id(raceId, Sort.unsorted()).isEmpty()) {
                throw new ConflictException("A race needs at least one runner before going live");
            }
            List<Race> previouslyVisible = raceRepository.findAllByVisibleOnLiveTrueAndIdNot(raceId);
            previouslyVisible.forEach(previous -> previous.setVisibleOnLive(false));
            raceRepository.saveAll(previouslyVisible);
            race.setVisibleOnLive(true);
        }

        race.setState(targetState);
        raceRepository.save(race);
        return snapshot(race);
    }

    @Transactional
    public RaceControlResponse selectRunners(Long raceId, List<Long> horseIds) {
        Race race = findLockedRace(raceId);
        if (race.getState() != RaceState.CREATED) {
            throw new ConflictException("Runners can only be changed while the race is a draft");
        }

        LinkedHashSet<Long> uniqueHorseIds = new LinkedHashSet<>(horseIds);
        if (uniqueHorseIds.size() != horseIds.size()) {
            throw new ConflictException("A horse can only be selected once");
        }

        Map<Long, Horse> horsesById = horseRepository.findAllById(uniqueHorseIds).stream()
                .collect(Collectors.toMap(Horse::getId, horse -> horse));
        if (horsesById.size() != uniqueHorseIds.size()) {
            throw new NotFoundException("Horse not found");
        }

        List<RaceEntry> existingEntries = raceEntryRepository.findAllByRace_Id(
                raceId, Sort.by("horseNumber").ascending());
        Map<Long, RaceEntry> existingByHorseId = existingEntries.stream()
                .collect(Collectors.toMap(entry -> entry.getHorse().getId(), entry -> entry));
        List<RaceEntry> removedEntries = existingEntries.stream()
                .filter(entry -> !uniqueHorseIds.contains(entry.getHorse().getId()))
                .toList();
        raceEntryRepository.deleteAll(removedEntries);

        Set<Integer> usedNumbers = existingEntries.stream()
                .filter(entry -> uniqueHorseIds.contains(entry.getHorse().getId()))
                .map(RaceEntry::getHorseNumber)
                .collect(Collectors.toSet());
        List<RaceEntry> selectedEntries = uniqueHorseIds.stream()
                .map(horseId -> {
                    RaceEntry existing = existingByHorseId.get(horseId);
                    if (existing != null) {
                        return existing;
                    }
                    RaceEntry entry = new RaceEntry();
                    entry.setRace(race);
                    entry.setHorse(horsesById.get(horseId));
                    entry.setHorseNumber(nextAvailableNumber(usedNumbers));
                    return entry;
                })
                .toList();
        raceEntryRepository.saveAll(selectedEntries);
        return snapshot(race);
    }

    @Transactional
    public RaceControlResponse clearLive(Long raceId) {
        Race race = findLockedRace(raceId);
        if (race.getState() != RaceState.FINISHED) {
            throw new ConflictException("Only a finished race can be removed from the live screen");
        }
        race.setVisibleOnLive(false);
        raceRepository.save(race);
        return snapshot(race);
    }

    @Transactional
    public RaceControlResponse publishResult(Long raceId, List<Long> orderedEntryIds) {
        Race race = findLockedRace(raceId);
        if (race.getState() != RaceState.BET_CLOSED) {
            throw new ConflictException("Bets must be closed before publishing the result");
        }

        List<RaceEntry> entries = raceEntryRepository.findAllByRace_Id(raceId, Sort.by("horseNumber").ascending());
        Set<Long> expectedIds = entries.stream().map(RaceEntry::getId).collect(Collectors.toSet());
        if (orderedEntryIds.size() != entries.size()
                || new HashSet<>(orderedEntryIds).size() != orderedEntryIds.size()
                || !expectedIds.equals(new HashSet<>(orderedEntryIds))) {
            throw new ConflictException("The finish order must contain every runner exactly once");
        }

        Map<Long, RaceEntry> entriesById = entries.stream()
                .collect(Collectors.toMap(RaceEntry::getId, entry -> entry));
        for (int index = 0; index < orderedEntryIds.size(); index++) {
            entriesById.get(orderedEntryIds.get(index)).setRank(index + 1);
        }

        Long winningEntryId = orderedEntryIds.getFirst();
        List<Bet> bets = betRepository.findAllByRaceEntry_Race_IdOrderByDateTimeBetAscIdAsc(raceId);
        bets.forEach(bet -> {
            if (bet.getRaceEntry().getId().equals(winningEntryId)) {
                bet.markAsWon();
            } else {
                bet.markAsLost();
            }
        });

        raceEntryRepository.saveAll(entries);
        betRepository.saveAll(bets);
        race.setState(RaceState.FINISHED);
        race.setFinishedAt(Instant.now());
        raceRepository.save(race);
        return snapshot(race);
    }

    private RaceControlResponse snapshot(Race race) {
        List<RaceEntry> entries = raceEntryRepository.findAllByRace_Id(
                race.getId(), Sort.by("horseNumber").ascending());
        Map<Long, Long> counts = betRepository.countByRaceEntryForRace(race.getId()).stream()
                .collect(Collectors.toMap(BetRepository.BetCountView::getRaceEntryId,
                        BetRepository.BetCountView::getBetCount));
        List<RaceControlEntryResponse> responses = entries.stream()
                .map(entry -> new RaceControlEntryResponse(entry.getId(), entry.getHorse().getId(),
                        entry.getHorse().getName(), entry.getHorseNumber(), entry.getRank(),
                        counts.getOrDefault(entry.getId(), 0L)))
                .toList();
        return new RaceControlResponse(race.getId(), race.getName(), race.getRaceImgUrl(), race.getState(),
                race.isVisibleOnLive(),
                responses, counts.values().stream().mapToLong(Long::longValue).sum(), Instant.now());
    }

    private Race findRace(Long id) {
        return raceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Race not found"));
    }

    private Race findLockedRace(Long id) {
        return raceRepository.findLockedById(id)
                .orElseThrow(() -> new NotFoundException("Race not found"));
    }

    private RaceState nextState(RaceState state) {
        return switch (state) {
            case CREATED -> RaceState.STANDBY;
            case STANDBY -> RaceState.BET_STARTING;
            case BET_STARTING -> RaceState.BETTING;
            case BETTING -> RaceState.BET_CLOSED;
            case BET_CLOSED -> RaceState.FINISHED;
            case FINISHED -> null;
        };
    }

    private int nextAvailableNumber(Set<Integer> usedNumbers) {
        int number = 1;
        while (usedNumbers.contains(number)) {
            number++;
        }
        usedNumbers.add(number);
        return number;
    }
}
