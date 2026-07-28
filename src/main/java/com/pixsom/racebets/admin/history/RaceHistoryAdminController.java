package com.pixsom.racebets.admin.history;

import com.pixsom.racebets.admin.history.dto.RaceHistoryDetailResponse;
import com.pixsom.racebets.admin.history.dto.RaceHistorySummaryResponse;
import com.pixsom.racebets.admin.race.dto.RaceResultCorrectionRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/race-history")
public class RaceHistoryAdminController {

    private final RaceHistoryService raceHistoryService;

    public RaceHistoryAdminController(RaceHistoryService raceHistoryService) {
        this.raceHistoryService = raceHistoryService;
    }

    @GetMapping
    public List<RaceHistorySummaryResponse> findAll() {
        return raceHistoryService.findAll();
    }

    @GetMapping("/{raceId}")
    public RaceHistoryDetailResponse findById(@PathVariable Long raceId) {
        return raceHistoryService.findById(raceId);
    }

    @PutMapping("/{raceId}/result")
    public RaceHistoryDetailResponse correctResult(@PathVariable Long raceId,
                                                   @Valid @RequestBody RaceResultCorrectionRequest request) {
        return raceHistoryService.correctResult(
                raceId, request.expectedOrderedEntryIds(), request.orderedEntryIds());
    }
}
