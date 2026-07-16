package com.pixsom.racebets.admin.race;

import com.pixsom.racebets.admin.ConflictException;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.entities.Bet;
import com.pixsom.racebets.entities.Horse;
import com.pixsom.racebets.entities.Race;
import com.pixsom.racebets.entities.RaceEntry;
import com.pixsom.racebets.enums.BetState;
import com.pixsom.racebets.enums.RaceState;
import com.pixsom.racebets.repositories.BetRepository;
import com.pixsom.racebets.repositories.HorseRepository;
import com.pixsom.racebets.repositories.RaceEntryRepository;
import com.pixsom.racebets.repositories.RaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaceWorkflowServiceTest {

    @Mock RaceRepository raceRepository;
    @Mock RaceEntryRepository raceEntryRepository;
    @Mock BetRepository betRepository;
    @Mock HorseRepository horseRepository;

    private RaceWorkflowService service;

    @BeforeEach
    void setUp() {
        service = new RaceWorkflowService(raceRepository, raceEntryRepository, betRepository, horseRepository);
    }

    @Test
    void transitionMakesCreatedRaceVisibleWhenItHasRunners() {
        Race race = race(1L, RaceState.CREATED);
        RaceEntry entry = entry(10L, race, 4, "Ourasi");
        when(raceRepository.findLockedById(1L)).thenReturn(Optional.of(race));
        when(raceRepository.existsByStateInAndIdNot(any(), any())).thenReturn(false);
        when(raceEntryRepository.findAllByRace_Id(any(), any(Sort.class))).thenReturn(List.of(entry));
        when(betRepository.countByRaceEntryForRace(1L)).thenReturn(List.of());

        var response = service.transition(1L, RaceState.STANDBY);

        assertThat(response.state()).isEqualTo(RaceState.STANDBY);
        assertThat(race.getState()).isEqualTo(RaceState.STANDBY);
        assertThat(race.isVisibleOnLive()).isTrue();
        verify(raceRepository).save(race);
    }

    @Test
    void transitionCannotSkipAWorkflowState() {
        Race race = race(1L, RaceState.CREATED);
        when(raceRepository.findLockedById(1L)).thenReturn(Optional.of(race));

        assertThatThrownBy(() -> service.transition(1L, RaceState.BETTING))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Invalid race state transition");
    }

    @Test
    void selectRunnersCreatesEntriesWithAutomaticNumbers() {
        Race race = race(1L, RaceState.CREATED);
        Horse ourasi = horse(20L, "Ourasi");
        Horse bellino = horse(21L, "Bellino II");
        when(raceRepository.findLockedById(1L)).thenReturn(Optional.of(race));
        when(horseRepository.findAllById(any())).thenReturn(List.of(ourasi, bellino));
        when(raceEntryRepository.findAllByRace_Id(any(), any(Sort.class))).thenReturn(List.of());
        when(betRepository.countByRaceEntryForRace(1L)).thenReturn(List.of());

        service.selectRunners(1L, List.of(20L, 21L));

        verify(raceEntryRepository).saveAll(org.mockito.ArgumentMatchers.argThat(entries -> {
            List<RaceEntry> saved = new java.util.ArrayList<>();
            entries.forEach(saved::add);
            return saved.size() == 2
                    && saved.get(0).getHorseNumber() == 1
                    && saved.get(1).getHorseNumber() == 2;
        }));
    }

    @Test
    void selectRunnersRejectsNonDraftRace() {
        Race race = race(1L, RaceState.STANDBY);
        when(raceRepository.findLockedById(1L)).thenReturn(Optional.of(race));

        assertThatThrownBy(() -> service.selectRunners(1L, List.of(20L)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Runners can only be changed while the race is a draft");
    }

    @Test
    void publishingResultRanksEntriesAndSettlesEveryBet() {
        Race race = race(1L, RaceState.BET_CLOSED);
        RaceEntry first = entry(10L, race, 4, "Ourasi");
        RaceEntry winner = entry(11L, race, 7, "Bellino II");
        Bet losingBet = bet(100L, first);
        Bet winningBet = bet(101L, winner);
        when(raceRepository.findLockedById(1L)).thenReturn(Optional.of(race));
        when(raceEntryRepository.findAllByRace_Id(any(), any(Sort.class))).thenReturn(List.of(first, winner));
        when(betRepository.findAllByRaceEntry_Race_IdOrderByDateTimeBetAscIdAsc(1L))
                .thenReturn(List.of(losingBet, winningBet));
        when(betRepository.countByRaceEntryForRace(1L)).thenReturn(List.of());

        var response = service.publishResult(1L, List.of(11L, 10L));

        assertThat(response.state()).isEqualTo(RaceState.FINISHED);
        assertThat(winner.getRank()).isEqualTo(1);
        assertThat(first.getRank()).isEqualTo(2);
        assertThat(winningBet.getState()).isEqualTo(BetState.WON);
        assertThat(losingBet.getState()).isEqualTo(BetState.LOST);
    }

    @Test
    void clearLiveKeepsFinishedRaceButRemovesItsResultFromPublicScreen() {
        Race race = race(1L, RaceState.FINISHED);
        race.setVisibleOnLive(true);
        when(raceRepository.findLockedById(1L)).thenReturn(Optional.of(race));
        when(raceEntryRepository.findAllByRace_Id(any(), any(Sort.class))).thenReturn(List.of());
        when(betRepository.countByRaceEntryForRace(1L)).thenReturn(List.of());

        var response = service.clearLive(1L);

        assertThat(response.state()).isEqualTo(RaceState.FINISHED);
        assertThat(response.visibleOnLive()).isFalse();
        verify(raceRepository).save(race);
    }

    private Race race(Long id, RaceState state) {
        Race race = new Race();
        ReflectionTestUtils.setField(race, "id", id);
        race.setName("Prix de Paris");
        race.setState(state);
        return race;
    }

    private RaceEntry entry(Long id, Race race, int number, String name) {
        Horse horse = horse(id + 100, name);
        RaceEntry entry = new RaceEntry();
        ReflectionTestUtils.setField(entry, "id", id);
        entry.setRace(race);
        entry.setHorse(horse);
        entry.setHorseNumber(number);
        return entry;
    }

    private Horse horse(Long id, String name) {
        Horse horse = new Horse();
        ReflectionTestUtils.setField(horse, "id", id);
        horse.setName(name);
        return horse;
    }

    private Bet bet(Long id, RaceEntry entry) {
        Bet bet = new Bet(entry, new AppUser());
        ReflectionTestUtils.setField(bet, "id", id);
        return bet;
    }
}
