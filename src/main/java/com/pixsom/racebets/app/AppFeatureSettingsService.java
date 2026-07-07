package com.pixsom.racebets.app;

import com.pixsom.racebets.admin.ConflictException;
import com.pixsom.racebets.app.dto.AppFeatureSettingsRequest;
import com.pixsom.racebets.app.dto.AppFeatureSettingsResponse;
import com.pixsom.racebets.repositories.AppFeatureSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppFeatureSettingsService {

    private static final Long SINGLETON_ID = 1L;

    private final AppFeatureSettingsRepository repository;

    public AppFeatureSettingsService(AppFeatureSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AppFeatureSettingsResponse current() {
        return toResponse(repository.findById(SINGLETON_ID).orElseGet(AppFeatureSettings::new));
    }

    @Transactional
    public AppFeatureSettingsResponse update(AppFeatureSettingsRequest request) {
        if (!request.bettingEnabled() && !request.quizEnabled()) {
            throw new ConflictException("At least one application feature must remain enabled");
        }

        AppFeatureSettings settings = repository.findById(SINGLETON_ID).orElseGet(AppFeatureSettings::new);
        settings.setBettingEnabled(request.bettingEnabled());
        settings.setQuizEnabled(request.quizEnabled());
        return toResponse(repository.save(settings));
    }

    private AppFeatureSettingsResponse toResponse(AppFeatureSettings settings) {
        return new AppFeatureSettingsResponse(settings.isBettingEnabled(), settings.isQuizEnabled());
    }
}
