package com.pixsom.racebets.betting;

import com.pixsom.racebets.betting.dto.BetHistoryResponse;
import com.pixsom.racebets.entities.Bet;
import com.pixsom.racebets.entities.RaceEntry;
import com.pixsom.racebets.enums.BetState;
import com.pixsom.racebets.enums.RaceState;
import com.pixsom.racebets.repositories.BetRepository;
import com.pixsom.racebets.repositories.RaceEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BetHistoryService {

    private final BetRepository betRepository;
    private final RaceEntryRepository raceEntryRepository;

    public BetHistoryService(BetRepository betRepository, RaceEntryRepository raceEntryRepository) {
        this.betRepository = betRepository;
        this.raceEntryRepository = raceEntryRepository;
    }

    @Transactional(readOnly = true)
    public boolean hasHistory(Long userId) {
        return betRepository.existsByUser_IdAndRaceEntry_Race_State(userId, RaceState.FINISHED);
    }

    @Transactional(readOnly = true)
    public List<BetHistoryResponse> findAll(Long userId) {
        List<Bet> bets = betRepository.findAllByUser_IdAndRaceEntry_Race_State(userId, RaceState.FINISHED);
        if (bets.isEmpty()) {
            return List.of();
        }

        List<Long> raceIds = bets.stream().map(bet -> bet.getRaceEntry().getRace().getId()).distinct().toList();
        Map<Long, RaceEntry> winnersByRace = raceEntryRepository.findAllByRace_IdInAndRank(raceIds, 1).stream()
                .collect(java.util.stream.Collectors.toMap(entry -> entry.getRace().getId(), entry -> entry));
        Map<Long, Integer> speedRanksByBet = speedRanks(raceIds);

        return bets.stream()
                .sorted(Comparator.comparing(this::finishedAt).reversed()
                        .thenComparing(bet -> bet.getRaceEntry().getRace().getId(), Comparator.reverseOrder()))
                .map(bet -> toResponse(bet, winnersByRace.get(bet.getRaceEntry().getRace().getId()),
                        speedRanksByBet.get(bet.getId())))
                .toList();
    }

    private Map<Long, Integer> speedRanks(List<Long> raceIds) {
        Map<Long, Integer> ranks = new HashMap<>();
        Map<Long, Integer> nextRankByRace = new HashMap<>();
        for (Bet bet : betRepository.findAllByRaceEntry_Race_IdInAndStateOrderByDateTimeBetAscIdAsc(raceIds, BetState.WON)) {
            Long raceId = bet.getRaceEntry().getRace().getId();
            int rank = nextRankByRace.merge(raceId, 1, Integer::sum);
            ranks.put(bet.getId(), rank);
        }
        return ranks;
    }

    private BetHistoryResponse toResponse(Bet bet, RaceEntry winner, Integer speedRank) {
        return new BetHistoryResponse(
                bet.getRaceEntry().getRace().getId(),
                bet.getRaceEntry().getRace().getName(),
                finishedAt(bet),
                bet.getRaceEntry().getHorse().getName(),
                winner == null ? "Non renseigné" : winner.getHorse().getName(),
                bet.getDateTimeBet(),
                bet.getState(),
                speedRank
        );
    }

    private Instant finishedAt(Bet bet) {
        var race = bet.getRaceEntry().getRace();
        return race.getFinishedAt() == null ? race.getUpdatedAt() : race.getFinishedAt();
    }
}
