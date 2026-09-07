package com.pixsom.racebets.auth;

import com.pixsom.racebets.auth.dto.LoginRequest;
import com.pixsom.racebets.auth.dto.LoginResponse;
import com.pixsom.racebets.auth.dto.RegisterRequest;
import com.pixsom.racebets.auth.dto.UserProfileResponse;
import com.pixsom.racebets.admin.ConflictException;
import com.pixsom.racebets.app.branding.AppBrandingService;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.enums.Role;
import com.pixsom.racebets.repositories.AppUserRepository;
import com.pixsom.racebets.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AppBrandingService brandingService;
    private final AuthAttemptLimiter attemptLimiter;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public AuthService(AppUserRepository appUserRepository, JwtService jwtService, PasswordEncoder passwordEncoder,
                       AppBrandingService brandingService, AuthAttemptLimiter attemptLimiter) {
        this.appUserRepository = appUserRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.brandingService = brandingService;
        this.attemptLimiter = attemptLimiter;
    }


    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (appUserRepository.existsByEmail(email)) {
            throw new ConflictException("A user already exists with this email");
        }

        AppUser user = new AppUser();
        user.setName(request.name().trim());
        user.setSurname(request.surname().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.accessCode()));
        user.setPresent(true);
        user.setRoles(Set.of(Role.USER));

        return tokenResponse(appUserRepository.save(user));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest loginRequest) {
        String email = normalizeEmail(loginRequest.email());
        attemptLimiter.acquire(email);
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        boolean accountRolesAreSafeForPasswordlessLogin = user.getRoles() != null
                && !user.getRoles().contains(Role.ADMIN);
        boolean passwordRequired = !accountRolesAreSafeForPasswordlessLogin
                || !brandingService.isPasswordlessLoginEnabled();
        String accessCode = loginRequest.accessCode();
        if (passwordRequired && (accessCode == null || accessCode.isBlank()
                || user.getPasswordHash() == null || !passwordEncoder.matches(accessCode, user.getPasswordHash()))) {
            throw new BadCredentialsException("Invalid credentials");
        }

        attemptLimiter.recordSuccess(email);
        return tokenResponse(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BadCredentialsException("Invalid credentials");
        }

        AppUser user = appUserRepository.findByEmail(normalizeEmail(authentication.getName()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse completeTutorial(Long userId) {
        AppUser user = appUserRepository.findLockedById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        user.setTutorialCompleted(true);
        return UserProfileResponse.from(user);
    }

    public LoginResponse tokenResponse(AppUser user) {
        String token = jwtService.generateToken(user);
        return new LoginResponse(token,
                "Bearer",
                jwtExpiration,
                user.getRoles(),
                UserProfileResponse.from(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
