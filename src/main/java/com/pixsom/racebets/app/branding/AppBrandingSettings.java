package com.pixsom.racebets.app.branding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "app_branding_settings")
public class AppBrandingSettings {

    @Id
    private Long id = 1L;

    @Column(name = "app_name", nullable = false, length = 120)
    private String appName = "Racebets";

    @Column(name = "login_title", nullable = false, length = 160)
    private String loginTitle = "Vivez la course, simplement.";

    @Column(name = "login_subtitle", nullable = false, length = 300)
    private String loginSubtitle = "Pariez en direct, suivez les résultats et retrouvez votre classement au même endroit.";

    @Column(name = "passwordless_login_enabled", nullable = false)
    private boolean passwordlessLoginEnabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme", nullable = false, length = 32)
    private AppBrandingTheme theme = AppBrandingTheme.DEFAULT;

    @Lob
    @Column(name = "image_data")
    private byte[] imageData;

    @Column(name = "image_content_type", length = 40)
    private String imageContentType;

    @Column(name = "image_version", nullable = false)
    private long imageVersion;
}
