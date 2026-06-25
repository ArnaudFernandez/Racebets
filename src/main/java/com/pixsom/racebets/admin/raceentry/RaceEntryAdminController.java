package com.pixsom.racebets.admin.raceentry;

import com.pixsom.racebets.admin.raceentry.dto.RaceEntryRequest;
import com.pixsom.racebets.admin.raceentry.dto.RaceEntryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/race-entries")
public class RaceEntryAdminController {

    private final RaceEntryService raceEntryService;

    public RaceEntryAdminController(RaceEntryService raceEntryService) {
        this.raceEntryService = raceEntryService;
    }

    @GetMapping
    public List<RaceEntryResponse> findAll(@RequestParam(required = false) Long raceId) {
        return raceEntryService.findAll(raceId);
    }

    @GetMapping("/{id}")
    public RaceEntryResponse findById(@PathVariable Long id) {
        return raceEntryService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RaceEntryResponse create(@Valid @RequestBody RaceEntryRequest request) {
        return raceEntryService.create(request);
    }

    @PutMapping("/{id}")
    public RaceEntryResponse update(@PathVariable Long id, @Valid @RequestBody RaceEntryRequest request) {
        return raceEntryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        raceEntryService.delete(id);
    }
}
