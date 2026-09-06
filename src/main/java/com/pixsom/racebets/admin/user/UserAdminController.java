package com.pixsom.racebets.admin.user;

import com.pixsom.racebets.admin.user.dto.AdminUserRequest;
import com.pixsom.racebets.admin.user.dto.AdminUserResponse;
import com.pixsom.racebets.admin.user.dto.UserImportPreviewResponse;
import com.pixsom.racebets.admin.user.dto.UserImportResultResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final UserImportService userImportService;

    public UserAdminController(UserAdminService userAdminService, UserImportService userImportService) {
        this.userAdminService = userAdminService;
        this.userImportService = userImportService;
    }

    @GetMapping
    public List<AdminUserResponse> findAll() {
        return userAdminService.findAll();
    }

    @GetMapping("/{id}")
    public AdminUserResponse findById(@PathVariable Long id) {
        return userAdminService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponse create(@Valid @RequestBody AdminUserRequest request) {
        return userAdminService.create(request);
    }

    @PutMapping("/{id}")
    public AdminUserResponse update(@PathVariable Long id, @Valid @RequestBody AdminUserRequest request) {
        return userAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userAdminService.delete(id);
    }

    @PostMapping(path = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserImportPreviewResponse previewImport(@RequestParam("file") MultipartFile file) {
        return userImportService.preview(file);
    }

    @PostMapping(path = "/import/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserImportResultResponse confirmImport(@RequestParam("file") MultipartFile file,
                                                  @RequestParam("fileDigest") String fileDigest,
                                                  @RequestParam("planFingerprint") String planFingerprint) {
        return userImportService.confirm(file, fileDigest, planFingerprint);
    }
}
