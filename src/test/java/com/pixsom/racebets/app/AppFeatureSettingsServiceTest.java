package com.pixsom.racebets.app;

import com.pixsom.racebets.admin.ConflictException;
import com.pixsom.racebets.app.dto.AppFeatureSettingsRequest;
import com.pixsom.racebets.repositories.AppFeatureSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppFeatureSettingsServiceTest {

    @Mock
    AppFeatureSettingsRepository repository;

    @Test
    void missingSettingsDefaultToBetting() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        var response = new AppFeatureSettingsService(repository).current();

        assertThat(response.activeMode()).isEqualTo(AppMode.BETTING);
    }

    @Test
    void updateReplacesTheSingleActiveMode() {
        AppFeatureSettings settings = new AppFeatureSettings();
        when(repository.findLockedById(1L)).thenReturn(Optional.of(settings));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = new AppFeatureSettingsService(repository)
                .update(new AppFeatureSettingsRequest(AppMode.WORD_CLOUD));

        assertThat(response.activeMode()).isEqualTo(AppMode.WORD_CLOUD);
        assertThat(settings.getActiveMode()).isEqualTo(AppMode.WORD_CLOUD);
        verify(repository).save(settings);
    }

    @Test
    void activeModeRequirementUsesLockedSingletonAndAllowsMatchingMode() {
        AppFeatureSettings settings = new AppFeatureSettings();
        settings.setActiveMode(AppMode.QUIZ);
        when(repository.findReadLockedById(1L)).thenReturn(Optional.of(settings));

        new AppFeatureSettingsService(repository).requireActiveMode(AppMode.QUIZ);

        verify(repository).findReadLockedById(1L);
    }

    @Test
    void activeModeRequirementRejectsInactiveMode() {
        AppFeatureSettings settings = new AppFeatureSettings();
        settings.setActiveMode(AppMode.BETTING);
        when(repository.findReadLockedById(1L)).thenReturn(Optional.of(settings));

        assertThatThrownBy(() -> new AppFeatureSettingsService(repository).requireActiveMode(AppMode.WORD_CLOUD))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Application mode WORD_CLOUD is not active");
    }
}
