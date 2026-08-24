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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import com.pixsom.racebets.admin.race.dto.RaceControlResponse;
import com.pixsom.racebets.admin.race.dto.RaceResultRequest;
import com.pixsom.racebets.admin.race.dto.RaceRunnersRequest;
import com.pixsom.racebets.admin.race.dto.RaceStateRequest;

@RestController
@RequestMapping("/api/admin/races")
public class RaceAdminController {

    private final RaceService raceService;
    private final RaceWorkflowService raceWorkflowService;

    public RaceAdminController(RaceService raceService, RaceWorkflowService raceWorkflowService) {
        this.raceService = raceService;
        this.raceWorkflowService = raceWorkflowService;
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

    @PostMapping(path = "/{id}/image", consumes = "multipart/form-data")
    public RaceResponse uploadImage(@PathVariable Long id, @RequestParam MultipartFile image) {
        return raceService.uploadImage(id, image);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        raceService.delete(id);
    }

    @GetMapping("/{id}/control")
    public RaceControlResponse control(@PathVariable Long id) {
        return raceWorkflowService.control(id);
    }

    @PostMapping("/{id}/state")
    public RaceControlResponse transition(@PathVariable Long id, @Valid @RequestBody RaceStateRequest request) {
        return raceWorkflowService.transition(id, request.state());
    }

    @PostMapping("/{id}/result")
    public RaceControlResponse publishResult(@PathVariable Long id, @Valid @RequestBody RaceResultRequest request) {
        return raceWorkflowService.publishResult(id, request.orderedEntryIds());
    }

    @PutMapping("/{id}/runners")
    public RaceControlResponse selectRunners(@PathVariable Long id, @Valid @RequestBody RaceRunnersRequest request) {
        return raceWorkflowService.selectRunners(id, request.horseIds());
    }

    @PostMapping("/{id}/clear-live")
    public RaceControlResponse clearLive(@PathVariable Long id) {
        return raceWorkflowService.clearLive(id);
    }
}
