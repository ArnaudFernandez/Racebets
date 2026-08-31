package com.pixsom.racebets.admin.history;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RaceHistoryAdminControllerTest {

    private RaceHistoryService raceHistoryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        raceHistoryService = mock(RaceHistoryService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RaceHistoryAdminController(raceHistoryService))
                .build();
    }

    @Test
    void correctResultDelegatesTheCompleteOrder() throws Exception {
        mockMvc.perform(put("/api/admin/race-history/4/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedOrderedEntryIds\":[10,11,12],\"orderedEntryIds\":[12,10,11]}"))
                .andExpect(status().isOk());

        verify(raceHistoryService).correctResult(4L, List.of(10L, 11L, 12L), List.of(12L, 10L, 11L));
    }

    @Test
    void correctResultRejectsAnEmptyOrder() throws Exception {
        mockMvc.perform(put("/api/admin/race-history/4/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedOrderedEntryIds\":[],\"orderedEntryIds\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
