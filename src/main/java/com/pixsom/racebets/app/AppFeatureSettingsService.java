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
        return toResponse(setActiveMode(request.activeMode()));
    }

    @Transactional
    public void activateMode(AppMode activeMode) {
        setActiveMode(activeMode);
    }

    private AppFeatureSettings setActiveMode(AppMode activeMode) {
        AppFeatureSettings settings = repository.findLockedById(SINGLETON_ID).orElseGet(AppFeatureSettings::new);
        settings.setActiveMode(activeMode);
        return repository.save(settings);
    }

    @Transactional
    public void requireActiveMode(AppMode requiredMode) {
        AppMode activeMode = repository.findReadLockedById(SINGLETON_ID)
                .map(AppFeatureSettings::getActiveMode)
                .orElse(AppMode.BETTING);
        requireMode(requiredMode, activeMode);
    }

    private void requireMode(AppMode requiredMode, AppMode activeMode) {
        if (activeMode != requiredMode) {
            throw new ConflictException("Application mode " + requiredMode + " is not active");
        }
    }

    private AppFeatureSettingsResponse toResponse(AppFeatureSettings settings) {
        return new AppFeatureSettingsResponse(settings.getActiveMode());
    }
}
