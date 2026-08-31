package com.pixsom.racebets.app.branding.dto;

public record AppBrandingResponse(
        String appName,
        String imageUrl,
        String loginTitle,
        String loginSubtitle
) {
}
