package com.pixsom.racebets.admin.race;

import com.pixsom.racebets.admin.race.dto.RaceRequest;
import com.pixsom.racebets.admin.race.dto.RaceResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/races")
public class RaceAdminController {

    private final RaceService raceService;

    public RaceAdminController(RaceService raceService) {
        this.raceService = raceService;
    }

    @GetMapping
    public List<RaceResponse> findAll() {
        return raceService.findAll();
    }

    @GetMapping("/{id}")
    public RaceResponse findById(@PathVariable Long id) {
        return raceService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RaceResponse create(@Valid @RequestBody RaceRequest request) {
        return raceService.create(request);
    }

    @PutMapping("/{id}")
    public RaceResponse update(@PathVariable Long id, @Valid @RequestBody RaceRequest request) {
        return raceService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        raceService.delete(id);
    }
}
