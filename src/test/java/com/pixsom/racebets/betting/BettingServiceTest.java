package com.pixsom.racebets.betting;

import com.pixsom.racebets.admin.ConflictException;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.entities.Bet;
import com.pixsom.racebets.entities.Horse;
import com.pixsom.racebets.entities.Race;
import com.pixsom.racebets.entities.RaceEntry;
import com.pixsom.racebets.enums.RaceState;
import com.pixsom.racebets.repositories.AppUserRepository;
import com.pixsom.racebets.repositories.BetRepository;
import com.pixsom.racebets.repositories.RaceEntryRepository;
import com.pixsom.racebets.repositories.RaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BettingServiceTest {

    @Mock RaceRepository raceRepository;
    @Mock RaceEntryRepository raceEntryRepository;
    @Mock BetRepository betRepository;
    @Mock AppUserRepository appUserRepository;

    private BettingService service;

    @BeforeEach
    void setUp() {
        service = new BettingService(raceRepository, raceEntryRepository, betRepository, appUserRepository);
    }

    @Test
    void rejectsBetWhenMarketIsNotOpen() {
        AppUser user = user(5L);
        Race race = race(1L, RaceState.BET_CLOSED);
        when(appUserRepository.findLockedById(5L)).thenReturn(Optional.of(user));
        when(raceRepository.findLockedById(1L)).thenReturn(Optional.of(race));

        assertThatThrownBy(() -> service.placeBet(1L, 10L, 5L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Betting is not open for this race");
    }

    @Test
    void firstSelectionCreatesServerTimestampedBet() {
        AppUser user = user(5L);
        Race race = race(1L, RaceState.BETTING);
        RaceEntry entry = entry(10L, race, "Ourasi");
        prepareOpenRace(user, race, entry);
        when(betRepository.findByUser_IdAndRaceEntry_Race_Id(5L, 1L)).thenReturn(Optional.empty());

        service.placeBet(1L, 10L, 5L);

        ArgumentCaptor<Bet> captor = ArgumentCaptor.forClass(Bet.class);
        verify(betRepository).save(captor.capture());
        assertThat(captor.getValue().getRaceEntry()).isSameAs(entry);
        assertThat(captor.getValue().getDateTimeBet()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void changingSelectionReplacesEntryAndRenewsTimestamp() {
        AppUser user = user(5L);
        Race race = race(1L, RaceState.BETTING);
        RaceEntry previous = entry(9L, race, "Jappeloup");
        RaceEntry replacement = entry(10L, race, "Ourasi");
        Bet bet = new Bet(previous, user);
        Instant oldTimestamp = Instant.parse("2025-01-01T00:00:00Z");
        ReflectionTestUtils.setField(bet, "dateTimeBet", oldTimestamp);
        when(appUserRepository.findLockedById(5L)).thenReturn(Optional.of(user));
        when(raceRepository.findLockedById(1L)).thenReturn(Optional.of(race));
        when(raceEntryRepository.findById(10L)).thenReturn(Optional.of(replacement));
        when(raceEntryRepository.findAllByRace_Id(any(), any(Sort.class))).thenReturn(List.of(previous, replacement));
        when(betRepository.countByRaceEntryForRace(1L)).thenReturn(List.of());
        when(betRepository.findByUser_IdAndRaceEntry_Race_Id(5L, 1L)).thenReturn(Optional.of(bet));

        service.placeBet(1L, 10L, 5L);

        assertThat(bet.getRaceEntry()).isSameAs(replacement);
        assertThat(bet.getDateTimeBet()).isAfter(oldTimestamp);
        verify(betRepository).save(bet);
    }

    private void prepareOpenRace(AppUser user, Race race, RaceEntry entry) {
        when(appUserRepository.findLockedById(5L)).thenReturn(Optional.of(user));
        when(raceRepository.findLockedById(1L)).thenReturn(Optional.of(race));
        when(raceEntryRepository.findById(10L)).thenReturn(Optional.of(entry));
        when(raceEntryRepository.findAllByRace_Id(any(), any(Sort.class))).thenReturn(List.of(entry));
        when(betRepository.countByRaceEntryForRace(1L)).thenReturn(List.of());
    }

    private AppUser user(Long id) {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Race race(Long id, RaceState state) {
        Race race = new Race();
        ReflectionTestUtils.setField(race, "id", id);
        race.setName("Prix de Paris");
        race.setState(state);
        return race;
    }

    private RaceEntry entry(Long id, Race race, String horseName) {
        Horse horse = new Horse();
        ReflectionTestUtils.setField(horse, "id", id + 100);
        horse.setName(horseName);
        RaceEntry entry = new RaceEntry();
        ReflectionTestUtils.setField(entry, "id", id);
        entry.setRace(race);
        entry.setHorse(horse);
        entry.setHorseNumber(id.intValue());
        return entry;
    }
}
