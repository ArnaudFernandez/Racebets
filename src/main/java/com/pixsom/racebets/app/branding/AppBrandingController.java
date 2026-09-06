package com.pixsom.racebets.app.branding;

import com.pixsom.racebets.app.branding.dto.AppBrandingImage;
import com.pixsom.racebets.app.branding.dto.AppBrandingResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequestMapping("/api")
public class AppBrandingController {

    private final AppBrandingService service;

    public AppBrandingController(AppBrandingService service) {
        this.service = service;
    }

    @GetMapping("/app/branding")
    public AppBrandingResponse current() {
        return service.current();
    }

    @GetMapping("/app/branding/image")
    public ResponseEntity<byte[]> image() {
        return service.image()
                .map(this::imageResponse)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping(path = "/admin/app/branding", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AppBrandingResponse update(@RequestParam("appName") String appName,
                                       @RequestParam("loginTitle") String loginTitle,
                                       @RequestParam("loginSubtitle") String loginSubtitle,
                                       @RequestParam("passwordlessLoginEnabled") boolean passwordlessLoginEnabled,
                                       @RequestParam("theme") AppBrandingTheme theme,
                                       @RequestParam(value = "image", required = false) MultipartFile image) {
        return service.update(appName, loginTitle, loginSubtitle, passwordlessLoginEnabled, theme, image);
    }

    private ResponseEntity<byte[]> imageResponse(AppBrandingImage image) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(image.data());
    }
}
