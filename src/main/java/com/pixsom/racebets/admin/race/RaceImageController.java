package com.pixsom.racebets.admin.race;

import com.pixsom.racebets.admin.NotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/race-images")
public class RaceImageController {

    private final RaceImageStorage raceImageStorage;

    public RaceImageController(RaceImageStorage raceImageStorage) {
        this.raceImageStorage = raceImageStorage;
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> get(@PathVariable String filename) {
        Resource image = raceImageStorage.load(filename);
        if (image == null) {
            throw new NotFoundException("Image introuvable.");
        }
        return ResponseEntity.ok()
                .contentType(raceImageStorage.contentType(filename))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(image);
    }
}
