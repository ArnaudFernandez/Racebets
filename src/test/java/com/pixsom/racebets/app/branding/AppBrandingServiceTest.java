package com.pixsom.racebets.app.branding;

import com.pixsom.racebets.admin.BadRequestException;
import com.pixsom.racebets.repositories.AppBrandingSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppBrandingServiceTest {

    @Mock
    AppBrandingSettingsRepository repository;

    @Test
    void missingSettingsUseDefaultBranding() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        var response = new AppBrandingService(repository).current();

        assertThat(response.appName()).isEqualTo("Racebets");
        assertThat(response.imageUrl()).isEqualTo("/logo_le_bouscat.png");
        assertThat(response.loginTitle()).isEqualTo("Vivez la course, simplement.");
        assertThat(response.loginSubtitle()).isEqualTo(
                "Pariez en direct, suivez les résultats et retrouvez votre classement au même endroit.");
        assertThat(response.passwordlessLoginEnabled()).isFalse();
        assertThat(response.theme()).isEqualTo(AppBrandingTheme.DEFAULT);
    }

    @Test
    void updateStoresNameAndImageAndReturnsVersionedUrl() {
        AppBrandingSettings settings = new AppBrandingSettings();
        when(repository.findLockedById(1L)).thenReturn(Optional.of(settings));
        when(repository.save(any(AppBrandingSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = new AppBrandingService(repository).update(
                "  Hippodrome  ",
                "  Vibrez ensemble  ",
                "  Une expérience en direct.  ",
                true,
                AppBrandingTheme.OLIFAN_GROUP,
                new MockMultipartFile("image", "brand.webp", "image/webp", new byte[]{1, 2, 3}));

        assertThat(settings.getAppName()).isEqualTo("Hippodrome");
        assertThat(settings.getImageData()).containsExactly(1, 2, 3);
        assertThat(settings.getImageContentType()).isEqualTo("image/webp");
        assertThat(settings.getLoginTitle()).isEqualTo("Vibrez ensemble");
        assertThat(settings.getLoginSubtitle()).isEqualTo("Une expérience en direct.");
        assertThat(settings.isPasswordlessLoginEnabled()).isTrue();
        assertThat(settings.getTheme()).isEqualTo(AppBrandingTheme.OLIFAN_GROUP);
        assertThat(response.theme()).isEqualTo(AppBrandingTheme.OLIFAN_GROUP);
        assertThat(response.imageUrl()).isEqualTo("/api/app/branding/image?v=1");
    }

    @Test
    void updateWithoutImageKeepsExistingImage() {
        AppBrandingSettings settings = new AppBrandingSettings();
        byte[] existingImage = new byte[]{4, 5};
        settings.setImageData(existingImage);
        settings.setImageContentType("image/png");
        settings.setImageVersion(7);
        when(repository.findLockedById(1L)).thenReturn(Optional.of(settings));
        when(repository.save(settings)).thenReturn(settings);

        new AppBrandingService(repository).update(
                "Nouveau nom", "Titre", "Sous-titre", false, AppBrandingTheme.DEFAULT, null);

        assertThat(settings.getAppName()).isEqualTo("Nouveau nom");
        assertThat(settings.getImageData()).isSameAs(existingImage);
        assertThat(settings.getImageVersion()).isEqualTo(7);
    }

    @Test
    void rejectsUnsupportedImage() {
        MockMultipartFile image = new MockMultipartFile("image", "brand.svg", "image/svg+xml", new byte[]{1});

        assertThatThrownBy(() -> new AppBrandingService(repository).update(
                "Racebets", "Titre", "Sous-titre", false, AppBrandingTheme.DEFAULT, image))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PNG, JPEG ou WebP");
    }

    @Test
    void rejectsBlankLoginCopy() {
        AppBrandingSettings settings = new AppBrandingSettings();
        when(repository.findLockedById(1L)).thenReturn(Optional.of(settings));

        assertThatThrownBy(() -> new AppBrandingService(repository).update(
                "Racebets", " ", "Sous-titre", false, AppBrandingTheme.DEFAULT, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("titre de connexion");
    }

    @Test
    void rejectsMissingTheme() {
        AppBrandingSettings settings = new AppBrandingSettings();
        when(repository.findLockedById(1L)).thenReturn(Optional.of(settings));

        assertThatThrownBy(() -> new AppBrandingService(repository).update(
                "Racebets", "Titre", "Sous-titre", false, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("thème");
    }

    @Test
    void passwordlessLoginCheckFailsClosedWithoutSettings() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThat(new AppBrandingService(repository).isPasswordlessLoginEnabled()).isFalse();
    }
}
