package com.pixsom.racebets.partner;

import com.pixsom.racebets.partner.dto.PartnerResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/partners")
public class PartnerAdminController {

    private final PartnerService partnerService;

    public PartnerAdminController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @GetMapping
    public List<PartnerResponse> findAll() {
        return partnerService.findAllAdmin();
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public PartnerResponse create(@RequestParam String name,
                                  @RequestParam boolean displayOnWaiting,
                                  @RequestParam MultipartFile logo) {
        return partnerService.create(name, displayOnWaiting, logo);
    }

    @PutMapping(path = "/{id}", consumes = "multipart/form-data")
    public PartnerResponse update(@PathVariable Long id,
                                  @RequestParam String name,
                                  @RequestParam boolean displayOnWaiting,
                                  @RequestParam(required = false) MultipartFile logo) {
        return partnerService.update(id, name, displayOnWaiting, logo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        partnerService.delete(id);
    }
}
