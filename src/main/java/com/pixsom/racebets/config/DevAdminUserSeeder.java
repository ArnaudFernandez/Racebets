package com.pixsom.racebets.config;

import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.enums.Role;
import com.pixsom.racebets.repositories.AppUserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.HashSet;

@Component
@EnableConfigurationProperties(DevAdminProperties.class)
@ConditionalOnProperty(prefix = "racebets.dev-admin", name = "enabled", havingValue = "true")
public class DevAdminUserSeeder implements ApplicationRunner {

    private final DevAdminProperties properties;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DevAdminUserSeeder(DevAdminProperties properties, AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (properties.email() == null || properties.accessCode() == null) {
            throw new IllegalStateException("Dev admin email and access code must be configured when dev admin seeding is enabled");
        }

        appUserRepository.findByEmail(properties.email()).ifPresentOrElse(
                this::ensureAdminRole,
                this::createAdminUser
        );
    }

    private void createAdminUser() {
        AppUser user = new AppUser();
        user.setName("Local");
        user.setSurname("Admin");
        user.setEmail(properties.email());
        user.setPasswordHash(passwordEncoder.encode(properties.accessCode()));
        user.setPresent(true);
        user.setRoles(Set.of(Role.ADMIN));

        appUserRepository.save(user);
    }

    private void ensureAdminRole(AppUser user) {
        if (user.getRoles().contains(Role.ADMIN)) {
            return;
        }

        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.add(Role.ADMIN);
        user.setRoles(roles);
        appUserRepository.save(user);
    }
}
