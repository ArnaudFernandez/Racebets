package com.pixsom.racebets.auth;

import com.pixsom.racebets.auth.dto.LoginRequest;
import com.pixsom.racebets.auth.dto.LoginResponse;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.repositories.AppUserRepository;
import com.pixsom.racebets.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public AuthService(AppUserRepository appUserRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }


    public LoginResponse login(LoginRequest loginRequest) {
        AppUser user = appUserRepository.findByEmail(loginRequest.email()).orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(loginRequest.accessCode(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user);
        return new LoginResponse(token,
                "Bearer",
                jwtExpiration,
                user.getRoles());
    }
}
