package com.pixsom.racebets.admin.horse;

import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.admin.horse.dto.HorseRequest;
import com.pixsom.racebets.admin.horse.dto.HorseResponse;
import com.pixsom.racebets.entities.Horse;
import com.pixsom.racebets.repositories.HorseRepository;
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
class HorseServiceTest {

    @Mock
    private HorseRepository horseRepository;

    private HorseService horseService;

    @BeforeEach
    void setUp() {
        horseService = new HorseService(horseRepository);
    }

    @Test
    void findAllReturnsHorsesSortedByName() {
        Horse horse = horse(1L, "Ourasi");
        when(horseRepository.findAll(any(Sort.class))).thenReturn(List.of(horse));

        List<HorseResponse> response = horseService.findAll();

        assertThat(response).containsExactly(new HorseResponse(1L, "Ourasi"));
        verify(horseRepository).findAll(Sort.by("name").ascending());
    }

    @Test
    void createPersistsTrimmedHorseName() {
        when(horseRepository.save(any(Horse.class))).thenAnswer(invocation -> {
            Horse saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 12L);
            return saved;
        });

        HorseResponse response = horseService.create(new HorseRequest("  Bold Eagle  "));

        assertThat(response).isEqualTo(new HorseResponse(12L, "Bold Eagle"));
    }

    @Test
    void updateChangesExistingHorseName() {
        Horse horse = horse(4L, "Old Name");
        when(horseRepository.findById(4L)).thenReturn(Optional.of(horse));
        when(horseRepository.save(horse)).thenReturn(horse);

        HorseResponse response = horseService.update(4L, new HorseRequest("New Name"));

        assertThat(response).isEqualTo(new HorseResponse(4L, "New Name"));
    }

    @Test
    void updateRejectsUnknownHorse() {
        when(horseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> horseService.update(99L, new HorseRequest("Missing")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Horse not found");
    }

    @Test
    void deleteRemovesExistingHorse() {
        Horse horse = horse(7L, "Ready Cash");
        when(horseRepository.findById(7L)).thenReturn(Optional.of(horse));

        horseService.delete(7L);

        verify(horseRepository).delete(horse);
    }

    private Horse horse(Long id, String name) {
        Horse horse = new Horse();
        ReflectionTestUtils.setField(horse, "id", id);
        horse.setName(name);
        return horse;
    }
}
