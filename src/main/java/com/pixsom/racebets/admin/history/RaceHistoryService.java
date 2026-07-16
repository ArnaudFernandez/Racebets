package com.pixsom.racebets.admin.history;

import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.admin.history.dto.RaceHistoryDetailResponse;
import com.pixsom.racebets.admin.history.dto.RaceHistoryResultResponse;
import com.pixsom.racebets.admin.history.dto.RaceHistorySummaryResponse;
import com.pixsom.racebets.admin.history.dto.RaceHistoryVoteResponse;
import com.pixsom.racebets.admin.history.dto.RaceHistoryWinnerResponse;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.entities.Bet;
import com.pixsom.racebets.entities.Race;
import com.pixsom.racebets.entities.RaceEntry;
import com.pixsom.racebets.enums.BetState;
import com.pixsom.racebets.enums.RaceState;
import com.pixsom.racebets.repositories.BetRepository;
import com.pixsom.racebets.repositories.RaceEntryRepository;
import com.pixsom.racebets.repositories.RaceRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class RaceHistoryService {

    private final RaceRepository raceRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final BetRepository betRepository;

    public RaceHistoryService(RaceRepository raceRepository, RaceEntryRepository raceEntryRepository,
                              BetRepository betRepository) {
        this.raceRepository = raceRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.betRepository = betRepository;
    }

    @Transactional(readOnly = true)
    public List<RaceHistorySummaryResponse> findAll() {
        List<Race> races = raceRepository.findAllByState(RaceState.FINISHED);
        Map<Long, List<RaceEntry>> entriesByRace = raceEntryRepository.findAllByRace_State(
                        RaceState.FINISHED, Sort.by("rank").ascending())
                .stream()
                .collect(Collectors.groupingBy(entry -> entry.getRace().getId()));
        Map<Long, List<Bet>> betsByRace = betRepository.findAllByRaceEntry_Race_StateOrderByDateTimeBetAscIdAsc(
                        RaceState.FINISHED)
                .stream()
                .collect(Collectors.groupingBy(bet -> bet.getRaceEntry().getRace().getId()));

        return races.stream()
                .sorted(Comparator.comparing(this::finishedAt).reversed())
                .map(race -> toSummary(
                        race,
                        entriesByRace.getOrDefault(race.getId(), List.of()),
                        betsByRace.getOrDefault(race.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RaceHistoryDetailResponse findById(Long raceId) {
        Race race = raceRepository.findById(raceId)
                .filter(candidate -> candidate.getState() == RaceState.FINISHED)
                .orElseThrow(() -> new NotFoundException("Finished race not found"));
        List<RaceEntry> entries = raceEntryRepository.findAllByRace_Id(raceId, Sort.by("rank").ascending());
        List<Bet> bets = betRepository.findAllByRaceEntry_Race_IdOrderByDateTimeBetAscIdAsc(raceId);
        Map<Long, Long> voteCounts = bets.stream()
                .collect(Collectors.groupingBy(bet -> bet.getRaceEntry().getId(), Collectors.counting()));
        List<Bet> winningBets = bets.stream().filter(bet -> bet.getState() == BetState.WON).toList();

        return new RaceHistoryDetailResponse(
                race.getId(),
                race.getName(),
                finishedAt(race),
                bets.size(),
                entries.stream().map(entry -> new RaceHistoryResultResponse(
                        entry.getRank(), entry.getHorseNumber(), entry.getHorse().getName(),
                        voteCounts.getOrDefault(entry.getId(), 0L))).toList(),
                IntStream.range(0, winningBets.size())
                        .mapToObj(index -> toWinner(winningBets.get(index), index + 1))
                        .toList(),
                bets.stream().map(this::toVote).toList()
        );
    }

    private RaceHistorySummaryResponse toSummary(Race race, List<RaceEntry> entries, List<Bet> bets) {
        String winningHorseName = entries.stream()
                .filter(entry -> Integer.valueOf(1).equals(entry.getRank()))
                .map(entry -> entry.getHorse().getName())
                .findFirst()
                .orElse("Unknown");
        return new RaceHistorySummaryResponse(
                race.getId(), race.getName(), finishedAt(race), entries.size(), bets.size(),
                bets.stream().filter(bet -> bet.getState() == BetState.WON).count(), winningHorseName);
    }

    private RaceHistoryVoteResponse toVote(Bet bet) {
        AppUser user = bet.getUser();
        return new RaceHistoryVoteResponse(
                bet.getId(), user.getId(), displayName(user), user.getEmail(),
                bet.getRaceEntry().getHorse().getName(), bet.getDateTimeBet(), bet.getState());
    }

    private RaceHistoryWinnerResponse toWinner(Bet bet, int speedRank) {
        AppUser user = bet.getUser();
        return new RaceHistoryWinnerResponse(
                speedRank, user.getId(), displayName(user), user.getEmail(),
                bet.getRaceEntry().getHorse().getName(), bet.getDateTimeBet());
    }

    private String displayName(AppUser user) {
        return (user.getName() + " " + user.getSurname()).trim();
    }

    private Instant finishedAt(Race race) {
        return race.getFinishedAt() != null ? race.getFinishedAt() : race.getUpdatedAt();
    }
}
