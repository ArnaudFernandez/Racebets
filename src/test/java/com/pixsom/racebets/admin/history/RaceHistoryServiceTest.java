package com.pixsom.racebets.admin.history;

import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.entities.Bet;
import com.pixsom.racebets.entities.Horse;
import com.pixsom.racebets.entities.Race;
import com.pixsom.racebets.entities.RaceEntry;
import com.pixsom.racebets.enums.BetState;
import com.pixsom.racebets.enums.RaceState;
import com.pixsom.racebets.repositories.BetRepository;
import com.pixsom.racebets.repositories.RaceEntryRepository;
import com.pixsom.racebets.repositories.RaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaceHistoryServiceTest {

    @Mock RaceRepository raceRepository;
    @Mock RaceEntryRepository raceEntryRepository;
    @Mock BetRepository betRepository;

    private RaceHistoryService service;

    @BeforeEach
    void setUp() {
        service = new RaceHistoryService(raceRepository, raceEntryRepository, betRepository);
    }

    @Test
    void historyIsSortedNewestFirstWithWinnerAndVoteCounts() {
        Race older = race(1L, "Prix Ancien", "2026-07-01T12:00:00Z");
        Race newer = race(2L, "Prix Recent", "2026-07-02T12:00:00Z");
        RaceEntry olderWinner = entry(10L, older, "Ourasi", 1);
        RaceEntry newerWinner = entry(20L, newer, "Bellino II", 1);
        Bet oldWinningBet = bet(100L, olderWinner, user(50L, "Alice"), BetState.WON, "2026-07-01T11:59:00Z");
        when(raceRepository.findAllByState(RaceState.FINISHED)).thenReturn(List.of(older, newer));
        when(raceEntryRepository.findAllByRace_State(any(), any(Sort.class)))
                .thenReturn(List.of(olderWinner, newerWinner));
        when(betRepository.findAllByRaceEntry_Race_StateOrderByDateTimeBetAscIdAsc(RaceState.FINISHED))
                .thenReturn(List.of(oldWinningBet));

        var history = service.findAll();

        assertThat(history).extracting("raceId").containsExactly(2L, 1L);
        assertThat(history.get(1).winningHorseName()).isEqualTo("Ourasi");
        assertThat(history.get(1).totalVotes()).isEqualTo(1);
        assertThat(history.get(1).winnerCount()).isEqualTo(1);
    }

    @Test
    void detailRanksWinningVotersFromFastestToSlowest() {
        Race race = race(1L, "Prix de Paris", "2026-07-02T12:00:00Z");
        RaceEntry winner = entry(10L, race, "Ourasi", 1);
        RaceEntry second = entry(11L, race, "Bellino II", 2);
        Bet fastest = bet(100L, winner, user(50L, "Alice"), BetState.WON, "2026-07-02T11:58:00Z");
        Bet slower = bet(101L, winner, user(51L, "Bruno"), BetState.WON, "2026-07-02T11:59:00Z");
        Bet lost = bet(102L, second, user(52L, "Chloe"), BetState.LOST, "2026-07-02T11:59:30Z");
        when(raceRepository.findById(1L)).thenReturn(Optional.of(race));
        when(raceEntryRepository.findAllByRace_Id(any(), any(Sort.class))).thenReturn(List.of(winner, second));
        when(betRepository.findAllByRaceEntry_Race_IdOrderByDateTimeBetAscIdAsc(1L))
                .thenReturn(List.of(fastest, slower, lost));

        var detail = service.findById(1L);

        assertThat(detail.winners()).extracting("userDisplayName").containsExactly("Alice Test", "Bruno Test");
        assertThat(detail.winners()).extracting("speedRank").containsExactly(1, 2);
        assertThat(detail.votes()).hasSize(3);
        assertThat(detail.result().getFirst().voteCount()).isEqualTo(2);
    }

    @Test
    void detailRejectsRaceThatIsNotFinished() {
        Race race = race(1L, "Prix", "2026-07-02T12:00:00Z");
        race.setState(RaceState.BETTING);
        when(raceRepository.findById(1L)).thenReturn(Optional.of(race));

        assertThatThrownBy(() -> service.findById(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Finished race not found");
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
        ReflectionTestUtils.setField(horse, "id", id + 100);
        horse.setName(horseName);
        RaceEntry entry = new RaceEntry();
        ReflectionTestUtils.setField(entry, "id", id);
        entry.setRace(race);
        entry.setHorse(horse);
        entry.setHorseNumber(rank);
        entry.setRank(rank);
        return entry;
    }

    private AppUser user(Long id, String name) {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", id);
        user.setName(name);
        user.setSurname("Test");
        user.setEmail(name.toLowerCase() + "@example.com");
        return user;
    }

    private Bet bet(Long id, RaceEntry entry, AppUser user, BetState state, String placedAt) {
        Bet bet = new Bet(entry, user);
        ReflectionTestUtils.setField(bet, "id", id);
        ReflectionTestUtils.setField(bet, "dateTimeBet", Instant.parse(placedAt));
        if (state == BetState.WON) bet.markAsWon();
        else bet.markAsLost();
        return bet;
    }
}
