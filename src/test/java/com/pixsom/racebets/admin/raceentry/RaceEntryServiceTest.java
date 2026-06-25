package com.pixsom.racebets.admin.raceentry;

import com.pixsom.racebets.admin.ConflictException;
import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.admin.raceentry.dto.RaceEntryRequest;
import com.pixsom.racebets.admin.raceentry.dto.RaceEntryResponse;
import com.pixsom.racebets.entities.Horse;
import com.pixsom.racebets.entities.Race;
import com.pixsom.racebets.entities.RaceEntry;
import com.pixsom.racebets.enums.RaceState;
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
class RaceEntryServiceTest {

    @Mock
    private RaceEntryRepository raceEntryRepository;

    @Mock
    private RaceRepository raceRepository;

    @Mock
    private HorseRepository horseRepository;

    private RaceEntryService raceEntryService;

    @BeforeEach
    void setUp() {
        raceEntryService = new RaceEntryService(raceEntryRepository, raceRepository, horseRepository);
    }

    @Test
    void findAllReturnsEntriesSortedByRaceAndHorseNumber() {
        RaceEntry entry = raceEntry(1L, race(10L, "Prix de Paris"), horse(20L, "Ourasi"), 4, null);
        when(raceEntryRepository.findAll(any(Sort.class))).thenReturn(List.of(entry));

        List<RaceEntryResponse> response = raceEntryService.findAll(null);

        assertThat(response).containsExactly(new RaceEntryResponse(1L, 10L, "Prix de Paris", 20L, "Ourasi", 4, null));
        verify(raceEntryRepository).findAll(Sort.by(Sort.Order.asc("race.name"), Sort.Order.asc("horseNumber")));
    }

    @Test
    void findAllCanFilterByRaceId() {
        RaceEntry entry = raceEntry(1L, race(10L, "Prix de Paris"), horse(20L, "Ourasi"), 4, null);
        when(raceEntryRepository.findAllByRace_Id(any(Long.class), any(Sort.class))).thenReturn(List.of(entry));

        List<RaceEntryResponse> response = raceEntryService.findAll(10L);

        assertThat(response).containsExactly(new RaceEntryResponse(1L, 10L, "Prix de Paris", 20L, "Ourasi", 4, null));
        verify(raceEntryRepository).findAllByRace_Id(10L, Sort.by("horseNumber").ascending());
    }

    @Test
    void createRegistersHorseForRace() {
        Race race = race(10L, "Prix de Paris");
        Horse horse = horse(20L, "Ourasi");
        when(raceRepository.findById(10L)).thenReturn(Optional.of(race));
        when(horseRepository.findById(20L)).thenReturn(Optional.of(horse));
        when(raceEntryRepository.existsByRace_IdAndHorse_Id(10L, 20L)).thenReturn(false);
        when(raceEntryRepository.existsByRace_IdAndHorseNumber(10L, 4)).thenReturn(false);
        when(raceEntryRepository.save(any(RaceEntry.class))).thenAnswer(invocation -> {
            RaceEntry saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 30L);
            return saved;
        });

        RaceEntryResponse response = raceEntryService.create(new RaceEntryRequest(10L, 20L, 4, null));

        assertThat(response).isEqualTo(new RaceEntryResponse(30L, 10L, "Prix de Paris", 20L, "Ourasi", 4, null));
    }

    @Test
    void createRejectsHorseAlreadyRegisteredForRace() {
        Race race = race(10L, "Prix de Paris");
        Horse horse = horse(20L, "Ourasi");
        when(raceRepository.findById(10L)).thenReturn(Optional.of(race));
        when(horseRepository.findById(20L)).thenReturn(Optional.of(horse));
        when(raceEntryRepository.existsByRace_IdAndHorse_Id(10L, 20L)).thenReturn(true);

        assertThatThrownBy(() -> raceEntryService.create(new RaceEntryRequest(10L, 20L, 4, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Horse is already registered for this race");
    }

    @Test
    void createRejectsHorseNumberAlreadyUsedForRace() {
        Race race = race(10L, "Prix de Paris");
        Horse horse = horse(20L, "Ourasi");
        when(raceRepository.findById(10L)).thenReturn(Optional.of(race));
        when(horseRepository.findById(20L)).thenReturn(Optional.of(horse));
        when(raceEntryRepository.existsByRace_IdAndHorse_Id(10L, 20L)).thenReturn(false);
        when(raceEntryRepository.existsByRace_IdAndHorseNumber(10L, 4)).thenReturn(true);

        assertThatThrownBy(() -> raceEntryService.create(new RaceEntryRequest(10L, 20L, 4, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Horse number is already used for this race");
    }

    @Test
    void updateChangesRaceEntry() {
        RaceEntry entry = raceEntry(30L, race(10L, "Old Race"), horse(20L, "Old Horse"), 4, null);
        Race race = race(11L, "New Race");
        Horse horse = horse(21L, "New Horse");
        when(raceEntryRepository.findById(30L)).thenReturn(Optional.of(entry));
        when(raceRepository.findById(11L)).thenReturn(Optional.of(race));
        when(horseRepository.findById(21L)).thenReturn(Optional.of(horse));
        when(raceEntryRepository.existsByRace_IdAndHorse_IdAndIdNot(11L, 21L, 30L)).thenReturn(false);
        when(raceEntryRepository.existsByRace_IdAndHorseNumberAndIdNot(11L, 7, 30L)).thenReturn(false);
        when(raceEntryRepository.save(entry)).thenReturn(entry);

        RaceEntryResponse response = raceEntryService.update(30L, new RaceEntryRequest(11L, 21L, 7, 1));

        assertThat(response).isEqualTo(new RaceEntryResponse(30L, 11L, "New Race", 21L, "New Horse", 7, 1));
    }

    @Test
    void updateRejectsUnknownRaceEntry() {
        when(raceEntryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> raceEntryService.update(99L, new RaceEntryRequest(10L, 20L, 4, null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Race entry not found");
    }

    @Test
    void deleteRemovesExistingRaceEntry() {
        RaceEntry entry = raceEntry(30L, race(10L, "Prix"), horse(20L, "Ourasi"), 4, null);
        when(raceEntryRepository.findById(30L)).thenReturn(Optional.of(entry));

        raceEntryService.delete(30L);

        verify(raceEntryRepository).delete(entry);
    }

    private RaceEntry raceEntry(Long id, Race race, Horse horse, int horseNumber, Integer rank) {
        RaceEntry entry = new RaceEntry();
        ReflectionTestUtils.setField(entry, "id", id);
        entry.setRace(race);
        entry.setHorse(horse);
        entry.setHorseNumber(horseNumber);
        entry.setRank(rank);
        return entry;
    }

    private Race race(Long id, String name) {
        Race race = new Race();
        ReflectionTestUtils.setField(race, "id", id);
        race.setName(name);
        race.setState(RaceState.CREATED);
        return race;
    }

    private Horse horse(Long id, String name) {
        Horse horse = new Horse();
        ReflectionTestUtils.setField(horse, "id", id);
        horse.setName(name);
        return horse;
    }
}
