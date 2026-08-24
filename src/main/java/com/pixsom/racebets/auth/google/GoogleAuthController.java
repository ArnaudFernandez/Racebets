package com.pixsom.racebets.auth.google;

import com.pixsom.racebets.auth.dto.LoginResponse;
import com.pixsom.racebets.auth.google.dto.AuthProvidersResponse;
import com.pixsom.racebets.auth.google.dto.OAuthCodeExchangeRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;
    private final GoogleAuthProperties properties;

    public GoogleAuthController(GoogleAuthService googleAuthService, GoogleAuthProperties properties) {
        this.googleAuthService = googleAuthService;
        this.properties = properties;
    }

    @GetMapping("/api/auth/providers")
    public ResponseEntity<AuthProvidersResponse> providers() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new AuthProvidersResponse(properties.enabled()));
    }

    @PostMapping("/api/auth/oauth/exchange")
    public ResponseEntity<LoginResponse> exchange(@Valid @RequestBody OAuthCodeExchangeRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(googleAuthService.exchange(request));
    }
}
