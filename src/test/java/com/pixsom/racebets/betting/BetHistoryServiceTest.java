package com.pixsom.racebets.betting;

import com.pixsom.racebets.app.AppFeatureSettingsService;
import com.pixsom.racebets.app.AppMode;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.entities.Bet;
import com.pixsom.racebets.entities.Horse;
import com.pixsom.racebets.entities.Race;
import com.pixsom.racebets.entities.RaceEntry;
import com.pixsom.racebets.enums.BetState;
import com.pixsom.racebets.enums.RaceState;
import com.pixsom.racebets.repositories.BetRepository;
import com.pixsom.racebets.repositories.RaceEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BetHistoryServiceTest {

    @Mock BetRepository betRepository;
    @Mock RaceEntryRepository raceEntryRepository;
    @Mock AppFeatureSettingsService featureSettingsService;

    private BetHistoryService service;

    @BeforeEach
    void setUp() {
        service = new BetHistoryService(betRepository, raceEntryRepository, featureSettingsService);
    }

    @Test
    void returnsOnlyCurrentUsersFinishedBetsWithWinnerAndSpeedRank() {
        Race older = race(10L, "Prix ancien", "2026-01-01T12:00:00Z");
        Race newer = race(20L, "Prix récent", "2026-02-01T12:00:00Z");
        RaceEntry olderWinner = entry(101L, older, "Ourasi", 1);
        RaceEntry newerWinner = entry(201L, newer, "Jappeloup", 1);
        RaceEntry newerLoser = entry(202L, newer, "Idéal du Gazeau", 2);
        AppUser currentUser = user(5L);
        AppUser fasterUser = user(6L);

        Bet olderBet = bet(1001L, olderWinner, currentUser, BetState.WON, "2026-01-01T11:00:02Z");
        Bet fasterWinner = bet(2001L, newerWinner, fasterUser, BetState.WON, "2026-02-01T11:00:01Z");
        Bet currentWinner = bet(2002L, newerWinner, currentUser, BetState.WON, "2026-02-01T11:00:02Z");

        when(betRepository.findAllByUser_IdAndRaceEntry_Race_State(5L, RaceState.FINISHED))
                .thenReturn(List.of(olderBet, currentWinner));
        when(raceEntryRepository.findAllByRace_IdInAndRank(List.of(10L, 20L), 1))
                .thenReturn(List.of(olderWinner, newerWinner));
        when(betRepository.findAllByRaceEntry_Race_IdInAndStateOrderByDateTimeBetAscIdAsc(
                List.of(10L, 20L), BetState.WON))
                .thenReturn(List.of(olderBet, fasterWinner, currentWinner));

        var history = service.findAll(5L);

        assertThat(history).extracting(item -> item.raceId()).containsExactly(20L, 10L);
        assertThat(history.getFirst().selectedHorseName()).isEqualTo("Jappeloup");
        assertThat(history.getFirst().winningHorseName()).isEqualTo("Jappeloup");
        assertThat(history.getFirst().speedRank()).isEqualTo(2);
        assertThat(history.getLast().speedRank()).isEqualTo(1);
    }

    @Test
    void losingBetHasNoSpeedRank() {
        Race race = race(20L, "Prix de Paris", "2026-02-01T12:00:00Z");
        RaceEntry winner = entry(201L, race, "Jappeloup", 1);
        RaceEntry loser = entry(202L, race, "Ourasi", 2);
        Bet bet = bet(2002L, loser, user(5L), BetState.LOST, "2026-02-01T11:00:02Z");

        when(betRepository.findAllByUser_IdAndRaceEntry_Race_State(5L, RaceState.FINISHED)).thenReturn(List.of(bet));
        when(raceEntryRepository.findAllByRace_IdInAndRank(List.of(20L), 1)).thenReturn(List.of(winner));
        when(betRepository.findAllByRaceEntry_Race_IdInAndStateOrderByDateTimeBetAscIdAsc(
                List.of(20L), BetState.WON)).thenReturn(List.of());

        var history = service.findAll(5L);

        assertThat(history.getFirst().state()).isEqualTo(BetState.LOST);
        assertThat(history.getFirst().selectedHorseName()).isEqualTo("Ourasi");
        assertThat(history.getFirst().winningHorseName()).isEqualTo("Jappeloup");
        assertThat(history.getFirst().speedRank()).isNull();
    }

    @Test
    void availabilityIsFalseWithoutFinishedParticipation() {
        when(betRepository.existsByUser_IdAndRaceEntry_Race_State(5L, RaceState.FINISHED)).thenReturn(false);

        assertThat(service.hasHistory(5L)).isFalse();
        org.mockito.Mockito.verify(featureSettingsService).requireActiveMode(AppMode.BETTING);
    }

    private Race race(Long id, String name, String finishedAt) {
        Race race = new Race();
        ReflectionTestUtils.setField(race, "id", id);
        race.setName(name);
        race.setState(RaceState.FINISHED);
        race.setFinishedAt(Instant.parse(finishedAt));
        return race;
    }

    private RaceEntry entry(Long id, Race race, String horseName, int rank) {
        Horse horse = new Horse();
        ReflectionTestUtils.setField(horse, "id", id + 1000);
        horse.setName(horseName);
        RaceEntry entry = new RaceEntry();
        ReflectionTestUtils.setField(entry, "id", id);
        entry.setRace(race);
        entry.setHorse(horse);
        entry.setHorseNumber(rank);
        entry.setRank(rank);
        return entry;
    }

    private AppUser user(Long id) {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Bet bet(Long id, RaceEntry entry, AppUser user, BetState state, String placedAt) {
        Bet bet = new Bet(entry, user);
        ReflectionTestUtils.setField(bet, "id", id);
        bet.setState(state);
        bet.setDateTimeBet(Instant.parse(placedAt));
        return bet;
    }
}
