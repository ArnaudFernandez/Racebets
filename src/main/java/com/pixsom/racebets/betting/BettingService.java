package com.pixsom.racebets.betting;

import com.pixsom.racebets.admin.ConflictException;
import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.betting.dto.LiveRaceResponse;
import com.pixsom.racebets.betting.dto.LiveRunnerResponse;
import com.pixsom.racebets.betting.dto.UserBetResponse;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.entities.Bet;
import com.pixsom.racebets.entities.Race;
import com.pixsom.racebets.entities.RaceEntry;
import com.pixsom.racebets.enums.BetState;
import com.pixsom.racebets.enums.RaceState;
import com.pixsom.racebets.repositories.AppUserRepository;
import com.pixsom.racebets.repositories.BetRepository;
import com.pixsom.racebets.repositories.RaceEntryRepository;
import com.pixsom.racebets.repositories.RaceRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BettingService {

    private final RaceRepository raceRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final BetRepository betRepository;
    private final AppUserRepository appUserRepository;

    public BettingService(RaceRepository raceRepository, RaceEntryRepository raceEntryRepository,
                          BetRepository betRepository, AppUserRepository appUserRepository) {
        this.raceRepository = raceRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.betRepository = betRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public Optional<LiveRaceResponse> currentRace(Long userId) {
        return visibleRace().map(race -> snapshot(race, userId));
    }

    @Transactional
    public LiveRaceResponse placeBet(Long raceId, Long raceEntryId, Long userId) {
        AppUser user = appUserRepository.findLockedById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Race race = raceRepository.findLockedById(raceId)
                .orElseThrow(() -> new NotFoundException("Race not found"));

        if (race.getState() != RaceState.BETTING) {
            throw new ConflictException("Betting is not open for this race");
        }

        RaceEntry entry = raceEntryRepository.findById(raceEntryId)
                .filter(candidate -> candidate.getRace().getId().equals(raceId))
                .orElseThrow(() -> new NotFoundException("Runner not found for this race"));

        Optional<Bet> existing = betRepository.findByUser_IdAndRaceEntry_Race_Id(userId, raceId);
        if (existing.isEmpty()) {
            betRepository.save(new Bet(entry, user));
        } else if (!existing.get().getRaceEntry().getId().equals(raceEntryId)) {
            existing.get().changeSelection(entry);
            betRepository.save(existing.get());
        }

        return snapshot(race, userId);
    }

    private Optional<Race> visibleRace() {
        return raceRepository.findAllByVisibleOnLiveTrue(Sort.by("updatedAt").descending())
                .stream()
                .findFirst();
    }

    private LiveRaceResponse snapshot(Race race, Long userId) {
        List<RaceEntry> entries = raceEntryRepository.findAllByRace_Id(
                race.getId(), Sort.by("horseNumber").ascending());
        Map<Long, Long> counts = betRepository.countByRaceEntryForRace(race.getId()).stream()
                .collect(Collectors.toMap(BetRepository.BetCountView::getRaceEntryId,
                        BetRepository.BetCountView::getBetCount));
        List<LiveRunnerResponse> runners = entries.stream()
                .map(entry -> new LiveRunnerResponse(entry.getId(), entry.getHorse().getId(),
                        entry.getHorse().getName(), entry.getHorseNumber(), entry.getRank(),
                        counts.getOrDefault(entry.getId(), 0L)))
                .toList();
        UserBetResponse userBet = betRepository.findByUser_IdAndRaceEntry_Race_Id(userId, race.getId())
                .map(bet -> toUserBet(bet, race.getState()))
                .orElse(null);

        return new LiveRaceResponse(race.getId(), race.getName(), race.getRaceImgUrl(), race.getState(),
                runners, userBet, counts.values().stream().mapToLong(Long::longValue).sum(), Instant.now());
    }

    private UserBetResponse toUserBet(Bet bet, RaceState raceState) {
        Integer speedRank = null;
        if (raceState == RaceState.FINISHED && bet.getState() == BetState.WON) {
            List<Bet> winners = betRepository.findAllByRaceEntry_Race_IdOrderByDateTimeBetAscIdAsc(
                            bet.getRaceEntry().getRace().getId()).stream()
                    .filter(candidate -> candidate.getState() == BetState.WON)
                    .toList();
            speedRank = java.util.stream.IntStream.range(0, winners.size())
                    .filter(index -> winners.get(index).getId().equals(bet.getId()))
                    .map(index -> index + 1)
                    .findFirst()
                    .orElseThrow();
        }
        return new UserBetResponse(bet.getRaceEntry().getId(), bet.getRaceEntry().getHorse().getName(),
                bet.getDateTimeBet(), bet.getState(), speedRank);
    }
}
