package com.pixsom.racebets.admin.race;

import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.admin.race.dto.RaceRequest;
import com.pixsom.racebets.admin.race.dto.RaceResponse;
import com.pixsom.racebets.entities.Race;
import com.pixsom.racebets.enums.RaceState;
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
class RaceServiceTest {

    @Mock
    private RaceRepository raceRepository;

    @Mock
    private RaceImageStorage raceImageStorage;

    private RaceService raceService;

    @BeforeEach
    void setUp() {
        raceService = new RaceService(raceRepository, raceImageStorage);
    }

    @Test
    void findAllReturnsRacesSortedByName() {
        Race race = race(1L, "Prix de Paris", null, RaceState.CREATED);
        when(raceRepository.findAll(any(Sort.class))).thenReturn(List.of(race));

        List<RaceResponse> response = raceService.findAll();

        assertThat(response).containsExactly(new RaceResponse(1L, "Prix de Paris", null, RaceState.CREATED));
        verify(raceRepository).findAll(Sort.by("id").ascending());
    }

    @Test
    void createPersistsTrimmedRaceWithCreatedStateByDefault() {
        when(raceRepository.save(any(Race.class))).thenAnswer(invocation -> {
            Race saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 12L);
            return saved;
        });

        RaceResponse response = raceService.create(new RaceRequest("  Prix d'Amerique  ", "  https://img/race.png  ", null));

        assertThat(response).isEqualTo(new RaceResponse(12L, "Prix d'Amerique", "https://img/race.png", RaceState.CREATED));
    }

    @Test
    void createAlwaysStartsInCreatedState() {
        when(raceRepository.save(any(Race.class))).thenAnswer(invocation -> {
            Race saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 13L);
            return saved;
        });

        RaceResponse response = raceService.create(new RaceRequest("Prix de Vincennes", null, RaceState.STANDBY));

        assertThat(response).isEqualTo(new RaceResponse(13L, "Prix de Vincennes", null, RaceState.CREATED));
    }

    @Test
    void updateKeepsExistingStateWhenRequestStateIsMissing() {
        Race race = race(4L, "Old Race", "https://img/old.png", RaceState.BETTING);
        when(raceRepository.findById(4L)).thenReturn(Optional.of(race));
        when(raceRepository.save(race)).thenReturn(race);

        RaceResponse response = raceService.update(4L, new RaceRequest("New Race", "", null));

        assertThat(response).isEqualTo(new RaceResponse(4L, "New Race", null, RaceState.BETTING));
    }

    @Test
    void updateCannotBypassTheRaceWorkflow() {
        Race race = race(5L, "Prix", null, RaceState.CREATED);
        when(raceRepository.findById(5L)).thenReturn(Optional.of(race));
        when(raceRepository.save(race)).thenReturn(race);

        RaceResponse response = raceService.update(5L, new RaceRequest("Prix", null, RaceState.BET_CLOSED));

        assertThat(response).isEqualTo(new RaceResponse(5L, "Prix", null, RaceState.CREATED));
    }

    @Test
    void uploadImageReplacesTheRaceImageAndDeletesTheOldManagedFile() {
        Race race = race(6L, "Prix", "/api/race-images/11111111-1111-1111-1111-111111111111.png", RaceState.CREATED);
        var image = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        when(raceRepository.findById(6L)).thenReturn(Optional.of(race));
        when(raceImageStorage.store(image)).thenReturn("/api/race-images/22222222-2222-2222-2222-222222222222.jpg");
        when(raceRepository.save(race)).thenReturn(race);

        RaceResponse response = raceService.uploadImage(6L, image);

        assertThat(response.raceImgUrl()).isEqualTo("/api/race-images/22222222-2222-2222-2222-222222222222.jpg");
        verify(raceImageStorage).delete("/api/race-images/11111111-1111-1111-1111-111111111111.png");
    }

    @Test
    void updateRejectsUnknownRace() {
        when(raceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> raceService.update(99L, new RaceRequest("Missing", null, null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Race not found");
    }

    @Test
    void deleteRemovesExistingRace() {
        Race race = race(7L, "Prix", null, RaceState.CREATED);
        when(raceRepository.findById(7L)).thenReturn(Optional.of(race));

        raceService.delete(7L);

        verify(raceRepository).delete(race);
    }

    private Race race(Long id, String name, String raceImgUrl, RaceState state) {
        Race race = new Race();
        ReflectionTestUtils.setField(race, "id", id);
        race.setName(name);
        race.setRaceImgUrl(raceImgUrl);
        race.setState(state);
        return race;
    }
}
