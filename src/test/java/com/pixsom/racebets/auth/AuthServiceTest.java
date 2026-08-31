package com.pixsom.racebets.auth;

import com.pixsom.racebets.auth.dto.LoginRequest;
import com.pixsom.racebets.auth.dto.LoginResponse;
import com.pixsom.racebets.auth.dto.UserProfileResponse;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.enums.Role;
import com.pixsom.racebets.repositories.AppUserRepository;
import com.pixsom.racebets.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(appUserRepository, jwtService, passwordEncoder);
        ReflectionTestUtils.setField(authService, "jwtExpiration", 3_600_000L);
    }

    @Test
    void loginReturnsBearerTokenWhenCredentialsAreValid() {
        AppUser user = new AppUser();
        user.setEmail("bettor@example.com");
        user.setPasswordHash("encoded-access-code");
        user.setRoles(Set.of(Role.USER));

        when(appUserRepository.findByEmail("bettor@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("DUPJEA", "encoded-access-code")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("signed.jwt.token");

        LoginResponse response = authService.login(new LoginRequest("bettor@example.com", "DUPJEA"));

        assertThat(response.token()).isEqualTo("signed.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3_600_000L);
        assertThat(response.roles()).containsExactly(Role.USER);
    }

    @Test
    void loginRejectsUnknownEmailWithGenericBadCredentials() {
        when(appUserRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@example.com", "DUPJEA")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void loginRejectsInvalidAccessCodeWithGenericBadCredentials() {
        AppUser user = new AppUser();
        user.setEmail("bettor@example.com");
        user.setPasswordHash("encoded-access-code");
        user.setRoles(Set.of(Role.USER));

        when(appUserRepository.findByEmail("bettor@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WRONG", "encoded-access-code")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("bettor@example.com", "WRONG")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(passwordEncoder).matches("WRONG", "encoded-access-code");
        verifyNoInteractions(jwtService);
    }

    @Test
    void completeTutorialPersistsTheFlagAndReturnsTheUpdatedProfile() {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", 7L);
        user.setRoles(Set.of(Role.USER));
        when(appUserRepository.findLockedById(7L)).thenReturn(Optional.of(user));

        UserProfileResponse response = authService.completeTutorial(7L);

        assertThat(user.isTutorialCompleted()).isTrue();
        assertThat(response.tutorialCompleted()).isTrue();
        verify(appUserRepository).findLockedById(7L);
    }
}
