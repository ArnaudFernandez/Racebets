package com.pixsom.racebets.app.branding;

import com.pixsom.racebets.admin.AdminExceptionHandler;
import com.pixsom.racebets.app.branding.dto.AppBrandingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppBrandingControllerTest {

    private AppBrandingService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AppBrandingService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AppBrandingController(service))
                .setControllerAdvice(new AdminExceptionHandler())
                .build();
    }

    @Test
    void currentBrandingIsPublic() throws Exception {
        when(service.current()).thenReturn(new AppBrandingResponse(
                "Hippodrome", "/api/app/branding/image?v=1", "Vibrez ensemble", "Une expérience en direct.",
                AppBrandingTheme.OLIFAN_GROUP));

        mockMvc.perform(get("/api/app/branding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appName").value("Hippodrome"))
                .andExpect(jsonPath("$.loginTitle").value("Vibrez ensemble"))
                .andExpect(jsonPath("$.loginSubtitle").value("Une expérience en direct."))
                .andExpect(jsonPath("$.theme").value("OLIFAN_GROUP"))
                .andExpect(jsonPath("$.imageUrl").value("/api/app/branding/image?v=1"));
    }

    @Test
    void updateAcceptsMultipartBranding() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "brand.png", "image/png", new byte[]{1});
        when(service.update(eq("Hippodrome"), eq("Vibrez ensemble"), eq("Une expérience en direct."),
                eq(AppBrandingTheme.OLIFAN_GROUP), any()))
                .thenReturn(new AppBrandingResponse(
                        "Hippodrome", "/api/app/branding/image?v=1", "Vibrez ensemble", "Une expérience en direct.",
                        AppBrandingTheme.OLIFAN_GROUP));

        mockMvc.perform(multipart("/api/admin/app/branding")
                        .file(image)
                        .param("appName", "Hippodrome")
                        .param("loginTitle", "Vibrez ensemble")
                        .param("loginSubtitle", "Une expérience en direct.")
                        .param("theme", "OLIFAN_GROUP")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appName").value("Hippodrome"))
                .andExpect(jsonPath("$.theme").value("OLIFAN_GROUP"));
    }

    @Test
    void updateRejectsUnknownTheme() throws Exception {
        mockMvc.perform(multipart("/api/admin/app/branding")
                        .param("appName", "Hippodrome")
                        .param("loginTitle", "Vibrez ensemble")
                        .param("loginSubtitle", "Une expérience en direct.")
                        .param("theme", "olifan")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest());
    }

    @Test
    void imageReturnsStoredBytesAndContentType() throws Exception {
        when(service.image()).thenReturn(Optional.of(new com.pixsom.racebets.app.branding.dto.AppBrandingImage("image/png", new byte[]{8, 9})));

        mockMvc.perform(get("/api/app/branding/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(new byte[]{8, 9}));
    }
}
