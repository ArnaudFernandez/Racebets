package com.pixsom.racebets.partner;

import com.pixsom.racebets.entities.Partner;
import com.pixsom.racebets.partner.dto.PartnerResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/partners")
public class PartnerPublicController {

    private final PartnerService partnerService;

    public PartnerPublicController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @GetMapping
    public List<PartnerResponse> findVisible() {
        return partnerService.findVisible();
    }

    @GetMapping("/{id}/logo")
    public ResponseEntity<byte[]> logo(@PathVariable Long id) {
        Partner partner = partnerService.findEntity(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(partner.getLogoContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(partner.getLogoData());
    }
}
