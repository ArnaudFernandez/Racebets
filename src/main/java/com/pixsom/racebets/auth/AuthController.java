package com.pixsom.racebets.auth;

import com.pixsom.racebets.auth.dto.LoginRequest;
import com.pixsom.racebets.auth.dto.LoginResponse;
import com.pixsom.racebets.auth.dto.RegisterRequest;
import com.pixsom.racebets.auth.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/api/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest loginRequest){
        return authService.login(loginRequest);
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/api/auth/me")
    public UserProfileResponse me(Authentication authentication) {
        return authService.currentUser(authentication);
    }
}
