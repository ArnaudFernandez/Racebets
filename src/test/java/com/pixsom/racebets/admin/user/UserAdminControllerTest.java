package com.pixsom.racebets.admin.user;

import com.pixsom.racebets.admin.AdminExceptionHandler;
import com.pixsom.racebets.admin.user.dto.UserImportPreviewResponse;
import com.pixsom.racebets.admin.user.dto.UserImportResultResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAdminControllerTest {

    private UserImportService importService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        importService = mock(UserImportService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new UserAdminController(mock(UserAdminService.class), importService))
                .setControllerAdvice(new AdminExceptionHandler())
                .build();
    }

    @Test
    void previewsAMultipartCsv() throws Exception {
        when(importService.preview(any())).thenReturn(new UserImportPreviewResponse(
                "file", "plan", 2, 1, 1, 0, 0, true, List.of()));
        MockMultipartFile file = new MockMultipartFile(
                "file", "participants.csv", "text/csv", "Email;Prénom;Nom".getBytes());

        mockMvc.perform(multipart("/api/admin/users/import/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.importable").value(true));

        verify(importService).preview(any());
    }

    @Test
    void confirmsThePreviewFingerprints() throws Exception {
        when(importService.confirm(any(), any(), any()))
                .thenReturn(new UserImportResultResponse(2, 1, 3));
        MockMultipartFile file = new MockMultipartFile(
                "file", "participants.csv", "text/csv", "Email;Prénom;Nom".getBytes());

        mockMvc.perform(multipart("/api/admin/users/import/confirm")
                        .file(file)
                        .param("fileDigest", "file-digest")
                        .param("planFingerprint", "plan-fingerprint"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.updatedCount").value(1))
                .andExpect(jsonPath("$.unchangedCount").value(3));

        verify(importService).confirm(any(), org.mockito.ArgumentMatchers.eq("file-digest"),
                org.mockito.ArgumentMatchers.eq("plan-fingerprint"));
    }
}
