package com.pixsom.racebets.app;

import com.pixsom.racebets.app.dto.AppFeatureSettingsRequest;
import com.pixsom.racebets.app.dto.AppFeatureSettingsResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AppFeatureSettingsController {

    private final AppFeatureSettingsService service;

    public AppFeatureSettingsController(AppFeatureSettingsService service) {
        this.service = service;
    }

    @GetMapping("/app/features")
    public AppFeatureSettingsResponse current() {
        return service.current();
    }

    @PutMapping("/admin/app/features")
    public AppFeatureSettingsResponse update(@Valid @RequestBody AppFeatureSettingsRequest request) {
        return service.update(request);
    }
}
