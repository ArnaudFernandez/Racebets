package com.pixsom.racebets.auth.google;

import com.pixsom.racebets.auth.AuthService;
import com.pixsom.racebets.auth.dto.LoginResponse;
import com.pixsom.racebets.auth.google.dto.OAuthCodeExchangeRequest;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.enums.Role;
import com.pixsom.racebets.repositories.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    @Mock
    private OAuthIdentityRepository identityRepository;
    @Mock
    private OAuthLoginCodeRepository loginCodeRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthService authService;
    @Mock
    private OidcUser oidcUser;

    private GoogleAuthService googleAuthService;

    @BeforeEach
    void setUp() {
        googleAuthService = new GoogleAuthService(
                identityRepository,
                loginCodeRepository,
                appUserRepository,
                passwordEncoder,
                authService,
                new GoogleAuthProperties(true, "client-id", "client-secret", Duration.ofMinutes(1))
        );
    }

    @Test
    void firstGoogleLoginCreatesRaceBetsUserAndDurableIdentity() {
        givenVerifiedGoogleIdentity("google-subject", "bettor@example.com");
        when(oidcUser.getGivenName()).thenReturn("Camille");
        when(oidcUser.getFamilyName()).thenReturn("Martin");
        when(identityRepository.findByIssuerAndSubject("https://accounts.google.com", "google-subject"))
                .thenReturn(Optional.empty());
        when(appUserRepository.findLockedByEmail("bettor@example.com")).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String code = googleAuthService.issueLoginCode(oidcUser);

        assertThat(code).hasSize(43);
        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("bettor@example.com");
        assertThat(userCaptor.getValue().getPasswordHash()).isNull();
        assertThat(userCaptor.getValue().getRoles()).containsExactly(Role.USER);

        ArgumentCaptor<OAuthIdentity> identityCaptor = ArgumentCaptor.forClass(OAuthIdentity.class);
        verify(identityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getSubject()).isEqualTo("google-subject");
        assertThat(identityCaptor.getValue().getUser()).isSameAs(userCaptor.getValue());
        verify(loginCodeRepository).save(any(OAuthLoginCode.class));
    }

    @Test
    void existingUnverifiedLocalAccountRequiresAccessCodeBeforeLinking() {
        givenVerifiedGoogleIdentity("google-subject", "bettor@example.com");
        AppUser existingUser = localUser();
        when(identityRepository.findByIssuerAndSubject("https://accounts.google.com", "google-subject"))
                .thenReturn(Optional.empty());
        when(appUserRepository.findLockedByEmail("bettor@example.com")).thenReturn(Optional.of(existingUser));

        String rawCode = googleAuthService.issueLoginCode(oidcUser);
        ArgumentCaptor<OAuthLoginCode> loginCodeCaptor = ArgumentCaptor.forClass(OAuthLoginCode.class);
        verify(loginCodeRepository).save(loginCodeCaptor.capture());
        OAuthLoginCode storedCode = loginCodeCaptor.getValue();
        when(loginCodeRepository.findLockedByCodeHash(storedCode.getCodeHash())).thenReturn(Optional.of(storedCode));

        assertThatThrownBy(() -> googleAuthService.exchange(new OAuthCodeExchangeRequest(rawCode, null)))
                .isInstanceOf(OAuthAccountLinkRequiredException.class);
        verify(identityRepository, never()).save(any(OAuthIdentity.class));

        LoginResponse expectedResponse = new LoginResponse("jwt", "Bearer", 3_600_000, Set.of(Role.USER), null);
        when(passwordEncoder.matches("local-access-code", "password-hash")).thenReturn(true);
        when(authService.tokenResponse(existingUser)).thenReturn(expectedResponse);

        LoginResponse response = googleAuthService.exchange(new OAuthCodeExchangeRequest(rawCode, "local-access-code"));

        assertThat(response).isSameAs(expectedResponse);
        verify(identityRepository).save(any(OAuthIdentity.class));
        verify(loginCodeRepository).delete(storedCode);
    }

    @Test
    void knownGoogleSubjectFindsSameRaceBetsAccountWithoutUsingChangedEmail() {
        givenVerifiedGoogleIdentity("google-subject", "new-email@example.com");
        AppUser existingUser = localUser();
        OAuthIdentity identity = new OAuthIdentity();
        identity.setUser(existingUser);
        when(identityRepository.findByIssuerAndSubject("https://accounts.google.com", "google-subject"))
                .thenReturn(Optional.of(identity));

        googleAuthService.issueLoginCode(oidcUser);

        verify(appUserRepository, never()).findLockedByEmail(anyString());
        ArgumentCaptor<OAuthLoginCode> codeCaptor = ArgumentCaptor.forClass(OAuthLoginCode.class);
        verify(loginCodeRepository).save(codeCaptor.capture());
        assertThat(codeCaptor.getValue().getUser()).isSameAs(existingUser);
        assertThat(codeCaptor.getValue().getPendingSubject()).isNull();
    }

    private void givenVerifiedGoogleIdentity(String subject, String email) {
        try {
            when(oidcUser.getIssuer()).thenReturn(URI.create("https://accounts.google.com").toURL());
        } catch (MalformedURLException exception) {
            throw new IllegalStateException(exception);
        }
        when(oidcUser.getSubject()).thenReturn(subject);
        when(oidcUser.getEmail()).thenReturn(email);
        when(oidcUser.getEmailVerified()).thenReturn(true);
    }

    private AppUser localUser() {
        AppUser user = new AppUser();
        user.setEmail("bettor@example.com");
        user.setPasswordHash("password-hash");
        user.setRoles(Set.of(Role.USER));
        return user;
    }
}
