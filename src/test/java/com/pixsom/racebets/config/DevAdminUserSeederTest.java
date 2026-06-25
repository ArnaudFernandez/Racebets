package com.pixsom.racebets.config;

import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.enums.Role;
import com.pixsom.racebets.repositories.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevAdminUserSeederTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DevAdminUserSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new DevAdminUserSeeder(
                new DevAdminProperties(true, "admin@racebets.local", "ADMIN-LOCAL-2026"),
                appUserRepository,
                passwordEncoder
        );
    }

    @Test
    void createsAdminWhenUserDoesNotExist() {
        when(appUserRepository.findByEmail("admin@racebets.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("ADMIN-LOCAL-2026")).thenReturn("encoded-access-code");

        seeder.run(null);

        verify(appUserRepository).save(any(AppUser.class));
    }

    @Test
    void preservesExistingRolesWhenAddingAdminRole() {
        AppUser user = new AppUser();
        user.setRoles(Set.of(Role.USER));
        when(appUserRepository.findByEmail("admin@racebets.local")).thenReturn(Optional.of(user));

        seeder.run(null);

        assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.USER, Role.ADMIN);
        verify(appUserRepository).save(user);
    }

    @Test
    void doesNotSaveExistingAdminAgain() {
        AppUser user = new AppUser();
        user.setRoles(Set.of(Role.ADMIN));
        when(appUserRepository.findByEmail("admin@racebets.local")).thenReturn(Optional.of(user));

        seeder.run(null);

        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    void rejectsMissingConfigurationWhenEnabled() {
        DevAdminUserSeeder invalidSeeder = new DevAdminUserSeeder(
                new DevAdminProperties(true, null, null),
                appUserRepository,
                passwordEncoder
        );

        assertThatThrownBy(() -> invalidSeeder.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Dev admin email and access code must be configured when dev admin seeding is enabled");
    }
}
