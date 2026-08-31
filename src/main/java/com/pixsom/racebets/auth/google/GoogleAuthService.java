package com.pixsom.racebets.auth.google;

import com.pixsom.racebets.auth.AuthService;
import com.pixsom.racebets.auth.dto.LoginResponse;
import com.pixsom.racebets.auth.google.dto.OAuthCodeExchangeRequest;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.enums.Role;
import com.pixsom.racebets.repositories.AppUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

@Service
public class GoogleAuthService {

    private static final String GOOGLE_ISSUER = "https://accounts.google.com";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OAuthIdentityRepository identityRepository;
    private final OAuthLoginCodeRepository loginCodeRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final GoogleAuthProperties properties;

    public GoogleAuthService(OAuthIdentityRepository identityRepository,
                             OAuthLoginCodeRepository loginCodeRepository,
                             AppUserRepository appUserRepository,
                             PasswordEncoder passwordEncoder,
                             AuthService authService,
                             GoogleAuthProperties properties) {
        this.identityRepository = identityRepository;
        this.loginCodeRepository = loginCodeRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.properties = properties;
    }

    @Transactional
    public String issueLoginCode(OidcUser oidcUser) {
        String issuer = oidcUser.getIssuer() == null ? null : oidcUser.getIssuer().toString();
        String subject = oidcUser.getSubject();
        String email = normalizeEmail(oidcUser.getEmail());

        if (!GOOGLE_ISSUER.equals(issuer)
                || subject == null || subject.isBlank()
                || email == null
                || !Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
            throw new BadCredentialsException("Invalid Google identity");
        }

        OAuthIdentity identity = identityRepository.findByIssuerAndSubject(issuer, subject).orElse(null);
        AppUser user;
        boolean linkConfirmationRequired = false;

        if (identity != null) {
            user = identity.getUser();
        } else {
            user = appUserRepository.findLockedByEmail(email).orElse(null);
            if (user == null) {
                user = createGoogleUser(oidcUser, email);
                identityRepository.save(newIdentity(user, issuer, subject, email));
            } else {
                linkConfirmationRequired = true;
            }
        }

        Instant now = Instant.now();
        loginCodeRepository.deleteByExpiresAtBefore(now);

        String rawCode = generateCode();
        OAuthLoginCode loginCode = new OAuthLoginCode();
        loginCode.setCodeHash(hash(rawCode));
        loginCode.setUser(user);
        loginCode.setExpiresAt(now.plus(properties.loginCodeTtl()));
        if (linkConfirmationRequired) {
            loginCode.setPendingIssuer(issuer);
            loginCode.setPendingSubject(subject);
            loginCode.setPendingEmail(email);
        }
        loginCodeRepository.save(loginCode);
        return rawCode;
    }

    @Transactional
    public LoginResponse exchange(OAuthCodeExchangeRequest request) {
        OAuthLoginCode loginCode = loginCodeRepository.findLockedByCodeHash(hash(request.code()))
                .orElseThrow(() -> new BadCredentialsException("Invalid OAuth login code"));

        if (!loginCode.getExpiresAt().isAfter(Instant.now())) {
            loginCodeRepository.delete(loginCode);
            throw new BadCredentialsException("Invalid OAuth login code");
        }

        if (loginCode.getPendingSubject() != null) {
            if (request.accessCode() == null || request.accessCode().isBlank()) {
                throw new OAuthAccountLinkRequiredException();
            }
            if (loginCode.getUser().getPasswordHash() == null
                    || !passwordEncoder.matches(request.accessCode(), loginCode.getUser().getPasswordHash())) {
                throw new BadCredentialsException("Invalid credentials");
            }

            identityRepository.save(newIdentity(
                    loginCode.getUser(),
                    loginCode.getPendingIssuer(),
                    loginCode.getPendingSubject(),
                    loginCode.getPendingEmail()
            ));
        }

        AppUser user = loginCode.getUser();
        loginCodeRepository.delete(loginCode);
        return authService.tokenResponse(user);
    }

    private AppUser createGoogleUser(OidcUser oidcUser, String email) {
        AppUser user = new AppUser();
        user.setName(profileValue(oidcUser.getGivenName(), "Utilisateur"));
        user.setSurname(profileValue(oidcUser.getFamilyName(), "Google"));
        user.setEmail(email);
        user.setPasswordHash(null);
        user.setPresent(true);
        user.setRoles(Set.of(Role.USER));
        return appUserRepository.save(user);
    }

    private OAuthIdentity newIdentity(AppUser user, String issuer, String subject, String email) {
        OAuthIdentity identity = new OAuthIdentity();
        identity.setUser(user);
        identity.setIssuer(issuer);
        identity.setSubject(subject);
        identity.setProvider("GOOGLE");
        identity.setEmailAtLink(email);
        identity.setEmailVerified(true);
        return identity;
    }

    private String profileValue(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized.substring(0, Math.min(normalized.length(), 80));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateCode() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
