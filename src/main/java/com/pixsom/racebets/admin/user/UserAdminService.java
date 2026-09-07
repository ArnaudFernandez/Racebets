package com.pixsom.racebets.admin.user;

import com.pixsom.racebets.admin.ConflictException;
import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.admin.user.dto.AdminUserRequest;
import com.pixsom.racebets.admin.user.dto.AdminUserResponse;
import com.pixsom.racebets.auth.google.OAuthIdentityRepository;
import com.pixsom.racebets.auth.google.OAuthLoginCodeRepository;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.enums.Role;
import com.pixsom.racebets.repositories.AppUserRepository;
import com.pixsom.racebets.repositories.BetRepository;
import com.pixsom.racebets.repositories.QuizParticipantRepository;
import com.pixsom.racebets.repositories.QuizSubmissionRepository;
import com.pixsom.racebets.repositories.WordCloudResponseRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class UserAdminService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final BetRepository betRepository;
    private final QuizParticipantRepository quizParticipantRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final WordCloudResponseRepository wordCloudResponseRepository;
    private final OAuthIdentityRepository oAuthIdentityRepository;
    private final OAuthLoginCodeRepository oAuthLoginCodeRepository;

    public UserAdminService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            BetRepository betRepository,
            QuizParticipantRepository quizParticipantRepository,
            QuizSubmissionRepository quizSubmissionRepository,
            WordCloudResponseRepository wordCloudResponseRepository,
            OAuthIdentityRepository oAuthIdentityRepository,
            OAuthLoginCodeRepository oAuthLoginCodeRepository) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.betRepository = betRepository;
        this.quizParticipantRepository = quizParticipantRepository;
        this.quizSubmissionRepository = quizSubmissionRepository;
        this.wordCloudResponseRepository = wordCloudResponseRepository;
        this.oAuthIdentityRepository = oAuthIdentityRepository;
        this.oAuthLoginCodeRepository = oAuthLoginCodeRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> findAll() {
        return appUserRepository.findAll(Sort.by("email").ascending())
                .stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse findById(Long id) {
        return AdminUserResponse.from(findUser(id));
    }

    @Transactional
    public AdminUserResponse create(AdminUserRequest request) {
        if (request.accessCode() == null || request.accessCode().isBlank()) {
            throw new ConflictException("An access code is required when creating a user");
        }

        String email = normalizeEmail(request.email());
        if (appUserRepository.existsByEmail(email)) {
            throw new ConflictException("A user already exists with this email");
        }

        AppUser user = new AppUser();
        applyRequest(user, request, email);
        user.setPasswordHash(passwordEncoder.encode(request.accessCode()));
        return AdminUserResponse.from(appUserRepository.save(user));
    }

    @Transactional
    public AdminUserResponse update(Long id, AdminUserRequest request) {
        AppUser user = findUser(id);
        String email = normalizeEmail(request.email());
        appUserRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("A user already exists with this email");
                });

        if (isLastAdmin(user) && !request.roles().contains(Role.ADMIN)) {
            throw new ConflictException("The last admin account cannot lose admin access");
        }

        applyRequest(user, request, email);
        if (request.accessCode() != null && !request.accessCode().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.accessCode()));
        }
        return AdminUserResponse.from(appUserRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        AppUser user = findUser(id);
        if (isLastAdmin(user)) {
            throw new ConflictException("The last admin account cannot be deleted");
        }

        oAuthLoginCodeRepository.deleteAllByUserId(id);
        oAuthIdentityRepository.deleteAllByUserId(id);
        wordCloudResponseRepository.deleteAllByUserId(id);
        quizSubmissionRepository.deleteAllByUserId(id);
        quizParticipantRepository.deleteAllByUserId(id);
        betRepository.deleteAllByUserId(id);
        appUserRepository.delete(user);
        appUserRepository.flush();
    }

    private void applyRequest(AppUser user, AdminUserRequest request, String email) {
        user.setName(request.name().trim());
        user.setSurname(request.surname().trim());
        user.setEmail(email);
        user.setBirthDate(request.birthDate());
        user.setPresent(request.present());
        user.setRoles(Set.copyOf(request.roles()));
    }

    private AppUser findUser(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private boolean isLastAdmin(AppUser user) {
        return user.getRoles().contains(Role.ADMIN) && countAdmins() == 1;
    }

    private long countAdmins() {
        return appUserRepository.findAll().stream()
                .filter(user -> user.getRoles().contains(Role.ADMIN))
                .count();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
