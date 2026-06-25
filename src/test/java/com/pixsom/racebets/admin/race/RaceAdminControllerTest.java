package com.pixsom.racebets.admin.race;

import com.pixsom.racebets.admin.AdminExceptionHandler;
import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.admin.race.dto.RaceRequest;
import com.pixsom.racebets.admin.race.dto.RaceResponse;
import com.pixsom.racebets.enums.RaceState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RaceAdminControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RaceService raceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        raceService = mock(RaceService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RaceAdminController(raceService))
                .setControllerAdvice(new AdminExceptionHandler())
                .build();
    }

    @Test
    void findAllReturnsRaces() throws Exception {
        when(raceService.findAll()).thenReturn(List.of(new RaceResponse(1L, "Prix de Paris", null, RaceState.CREATED)));

        mockMvc.perform(get("/api/admin/races"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Prix de Paris"))
                .andExpect(jsonPath("$[0].state").value("CREATED"));
    }

    @Test
    void createReturnsCreatedRace() throws Exception {
        when(raceService.create(any(RaceRequest.class)))
                .thenReturn(new RaceResponse(2L, "Prix d'Amerique", "https://img/race.png", RaceState.STANDBY));

        mockMvc.perform(post("/api/admin/races")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RaceRequest("Prix d'Amerique", "https://img/race.png", RaceState.STANDBY))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("Prix d'Amerique"))
                .andExpect(jsonPath("$.raceImgUrl").value("https://img/race.png"))
                .andExpect(jsonPath("$.state").value("STANDBY"));
    }

    @Test
    void createRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/admin/races")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RaceRequest("", null, RaceState.CREATED))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReturnsUpdatedRace() throws Exception {
        when(raceService.update(any(Long.class), any(RaceRequest.class)))
                .thenReturn(new RaceResponse(3L, "Prix de Vincennes", null, RaceState.BETTING));

        mockMvc.perform(put("/api/admin/races/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RaceRequest("Prix de Vincennes", null, RaceState.BETTING))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.name").value("Prix de Vincennes"))
                .andExpect(jsonPath("$.state").value("BETTING"));
    }

    @Test
    void findByIdReturnsNotFoundForUnknownRace() throws Exception {
        when(raceService.findById(99L)).thenThrow(new NotFoundException("Race not found"));

        mockMvc.perform(get("/api/admin/races/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/races/4"))
                .andExpect(status().isNoContent());

        verify(raceService).delete(4L);
    }
}
