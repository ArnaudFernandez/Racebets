package com.pixsom.racebets.admin.horse;

import com.pixsom.racebets.admin.AdminExceptionHandler;
import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.admin.horse.dto.HorseRequest;
import com.pixsom.racebets.admin.horse.dto.HorseResponse;
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

class HorseAdminControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HorseService horseService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        horseService = mock(HorseService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new HorseAdminController(horseService))
                .setControllerAdvice(new AdminExceptionHandler())
                .build();
    }

    @Test
    void findAllReturnsHorses() throws Exception {
        when(horseService.findAll()).thenReturn(List.of(new HorseResponse(1L, "Ourasi")));

        mockMvc.perform(get("/api/admin/horses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Ourasi"));
    }

    @Test
    void createReturnsCreatedHorse() throws Exception {
        when(horseService.create(any(HorseRequest.class))).thenReturn(new HorseResponse(2L, "Bold Eagle"));

        mockMvc.perform(post("/api/admin/horses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HorseRequest("Bold Eagle"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("Bold Eagle"));
    }

    @Test
    void createRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/admin/horses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HorseRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReturnsUpdatedHorse() throws Exception {
        when(horseService.update(any(Long.class), any(HorseRequest.class)))
                .thenReturn(new HorseResponse(3L, "Ready Cash"));

        mockMvc.perform(put("/api/admin/horses/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HorseRequest("Ready Cash"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.name").value("Ready Cash"));
    }

    @Test
    void findByIdReturnsNotFoundForUnknownHorse() throws Exception {
        when(horseService.findById(99L)).thenThrow(new NotFoundException("Horse not found"));

        mockMvc.perform(get("/api/admin/horses/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/horses/4"))
                .andExpect(status().isNoContent());

        verify(horseService).delete(4L);
    }
}
