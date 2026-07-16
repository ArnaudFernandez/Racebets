package com.pixsom.racebets.admin.raceentry;

import com.pixsom.racebets.admin.AdminExceptionHandler;
import com.pixsom.racebets.admin.ConflictException;
import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.admin.raceentry.dto.RaceEntryRequest;
import com.pixsom.racebets.admin.raceentry.dto.RaceEntryResponse;
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

class RaceEntryAdminControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RaceEntryService raceEntryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        raceEntryService = mock(RaceEntryService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RaceEntryAdminController(raceEntryService))
                .setControllerAdvice(new AdminExceptionHandler())
                .build();
    }

    @Test
    void findAllReturnsRaceEntries() throws Exception {
        when(raceEntryService.findAll(null))
                .thenReturn(List.of(new RaceEntryResponse(1L, 10L, "Prix de Paris", 20L, "Ourasi", 4, null)));

        mockMvc.perform(get("/api/admin/race-entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].raceId").value(10L))
                .andExpect(jsonPath("$[0].raceName").value("Prix de Paris"))
                .andExpect(jsonPath("$[0].horseId").value(20L))
                .andExpect(jsonPath("$[0].horseName").value("Ourasi"))
                .andExpect(jsonPath("$[0].horseNumber").value(4));
    }

    @Test
    void findAllCanFilterByRaceId() throws Exception {
        when(raceEntryService.findAll(10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/race-entries?raceId=10"))
                .andExpect(status().isOk());

        verify(raceEntryService).findAll(10L);
    }

    @Test
    void createReturnsCreatedRaceEntry() throws Exception {
        when(raceEntryService.create(any(RaceEntryRequest.class)))
                .thenReturn(new RaceEntryResponse(2L, 10L, "Prix de Paris", 20L, "Ourasi", 4, null));

        mockMvc.perform(post("/api/admin/race-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RaceEntryRequest(10L, 20L, 4))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.horseNumber").value(4));
    }

    @Test
    void createRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/admin/race-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RaceEntryRequest(10L, 20L, 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReturnsConflictWhenBusinessInvariantIsBroken() throws Exception {
        when(raceEntryService.create(any(RaceEntryRequest.class)))
                .thenThrow(new ConflictException("Horse number is already used for this race"));

        mockMvc.perform(post("/api/admin/race-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RaceEntryRequest(10L, 20L, 4))))
                .andExpect(status().isConflict());
    }

    @Test
    void updateReturnsUpdatedRaceEntry() throws Exception {
        when(raceEntryService.update(any(Long.class), any(RaceEntryRequest.class)))
                .thenReturn(new RaceEntryResponse(3L, 10L, "Prix de Paris", 20L, "Ourasi", 4, 1));

        mockMvc.perform(put("/api/admin/race-entries/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RaceEntryRequest(10L, 20L, 4))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.rank").value(1));
    }

    @Test
    void findByIdReturnsNotFoundForUnknownRaceEntry() throws Exception {
        when(raceEntryService.findById(99L)).thenThrow(new NotFoundException("Race entry not found"));

        mockMvc.perform(get("/api/admin/race-entries/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/race-entries/4"))
                .andExpect(status().isNoContent());

        verify(raceEntryService).delete(4L);
    }
}
