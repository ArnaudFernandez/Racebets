package com.pixsom.racebets.admin.horse;

import com.pixsom.racebets.admin.horse.dto.HorseRequest;
import com.pixsom.racebets.admin.horse.dto.HorseResponse;
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
@RequestMapping("/api/admin/horses")
public class HorseAdminController {

    private final HorseService horseService;

    public HorseAdminController(HorseService horseService) {
        this.horseService = horseService;
    }

    @GetMapping
    public List<HorseResponse> findAll() {
        return horseService.findAll();
    }

    @GetMapping("/{id}")
    public HorseResponse findById(@PathVariable Long id) {
        return horseService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HorseResponse create(@Valid @RequestBody HorseRequest request) {
        return horseService.create(request);
    }

    @PutMapping("/{id}")
    public HorseResponse update(@PathVariable Long id, @Valid @RequestBody HorseRequest request) {
        return horseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        horseService.delete(id);
    }
}
