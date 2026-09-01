package com.pixsom.racebets.app.branding;

import com.pixsom.racebets.admin.BadRequestException;
import com.pixsom.racebets.app.branding.dto.AppBrandingImage;
import com.pixsom.racebets.app.branding.dto.AppBrandingResponse;
import com.pixsom.racebets.repositories.AppBrandingSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Service
public class AppBrandingService {

    public static final String DEFAULT_APP_NAME = "Racebets";
    public static final String DEFAULT_IMAGE_URL = "/logo_le_bouscat.png";
    public static final String DEFAULT_LOGIN_TITLE = "Vivez la course, simplement.";
    public static final String DEFAULT_LOGIN_SUBTITLE =
            "Pariez en direct, suivez les résultats et retrouvez votre classement au même endroit.";
    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp");
    private static final Long SINGLETON_ID = 1L;

    private final AppBrandingSettingsRepository repository;

    public AppBrandingService(AppBrandingSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AppBrandingResponse current() {
        return toResponse(repository.findById(SINGLETON_ID).orElseGet(AppBrandingSettings::new));
    }

    @Transactional
    public AppBrandingResponse update(String appName, String loginTitle, String loginSubtitle,
                                      AppBrandingTheme theme, MultipartFile image) {
        AppBrandingSettings settings = repository.findLockedById(SINGLETON_ID).orElseGet(AppBrandingSettings::new);
        if (theme == null) {
            throw new BadRequestException("Le thème de l'application est obligatoire.");
        }
        settings.setAppName(normalizeRequiredText(
                appName, 120, "Le nom de l'application est obligatoire et limité à 120 caractères."));
        settings.setLoginTitle(normalizeRequiredText(
                loginTitle, 160, "Le titre de connexion est obligatoire et limité à 160 caractères."));
        settings.setLoginSubtitle(normalizeRequiredText(
                loginSubtitle, 300, "Le sous-titre de connexion est obligatoire et limité à 300 caractères."));
        settings.setTheme(theme);
        if (image != null && !image.isEmpty()) {
            applyImage(settings, image);
        }
        return toResponse(repository.save(settings));
    }

    @Transactional(readOnly = true)
    public Optional<AppBrandingImage> image() {
        return repository.findById(SINGLETON_ID)
                .filter(settings -> settings.getImageData() != null && settings.getImageData().length > 0)
                .map(settings -> new AppBrandingImage(settings.getImageContentType(), settings.getImageData()));
    }

    private String normalizeRequiredText(String value, int maxLength, String errorMessage) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new BadRequestException(errorMessage);
        }
        return normalized;
    }

    private void applyImage(AppBrandingSettings settings, MultipartFile image) {
        if (image.getSize() > MAX_IMAGE_SIZE || !ALLOWED_IMAGE_TYPES.contains(image.getContentType())) {
            throw new BadRequestException("L'image doit être une image PNG, JPEG ou WebP de 2 Mo maximum.");
        }
        try {
            settings.setImageData(image.getBytes());
            settings.setImageContentType(image.getContentType());
            settings.setImageVersion(settings.getImageVersion() + 1);
        } catch (IOException exception) {
            throw new BadRequestException("L'image n'a pas pu être lue.");
        }
    }

    private AppBrandingResponse toResponse(AppBrandingSettings settings) {
        String imageUrl = settings.getImageData() == null || settings.getImageData().length == 0
                ? DEFAULT_IMAGE_URL
                : "/api/app/branding/image?v=" + settings.getImageVersion();
        return new AppBrandingResponse(
                settings.getAppName() == null ? DEFAULT_APP_NAME : settings.getAppName(),
                imageUrl,
                settings.getLoginTitle() == null ? DEFAULT_LOGIN_TITLE : settings.getLoginTitle(),
                settings.getLoginSubtitle() == null ? DEFAULT_LOGIN_SUBTITLE : settings.getLoginSubtitle(),
                settings.getTheme() == null ? AppBrandingTheme.DEFAULT : settings.getTheme()
        );
    }
}
