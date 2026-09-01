package com.pixsom.racebets.app.branding.dto;

import com.pixsom.racebets.app.branding.AppBrandingTheme;

public record AppBrandingResponse(
        String appName,
        String imageUrl,
        String loginTitle,
        String loginSubtitle,
        AppBrandingTheme theme
) {
}
