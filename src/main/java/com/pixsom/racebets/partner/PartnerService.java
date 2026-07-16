package com.pixsom.racebets.partner;

import com.pixsom.racebets.admin.BadRequestException;
import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.entities.Partner;
import com.pixsom.racebets.partner.dto.PartnerResponse;
import com.pixsom.racebets.repositories.PartnerRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
public class PartnerService {

    private static final long MAX_LOGO_SIZE = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private final PartnerRepository partnerRepository;

    public PartnerService(PartnerRepository partnerRepository) {
        this.partnerRepository = partnerRepository;
    }

    @Transactional(readOnly = true)
    public List<PartnerResponse> findAllAdmin() {
        return partnerRepository.findAll(Sort.by("id").ascending()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PartnerResponse> findVisible() {
        return partnerRepository.findAllByDisplayOnWaitingTrue(Sort.by("id").ascending()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PartnerResponse create(String name, boolean displayOnWaiting, MultipartFile logo) {
        Partner partner = new Partner();
        apply(partner, name, displayOnWaiting, logo, true);
        return toResponse(partnerRepository.save(partner));
    }

    @Transactional
    public PartnerResponse update(Long id, String name, boolean displayOnWaiting, MultipartFile logo) {
        Partner partner = findEntity(id);
        apply(partner, name, displayOnWaiting, logo, false);
        return toResponse(partnerRepository.save(partner));
    }

    @Transactional
    public void delete(Long id) {
        Partner partner = findEntity(id);
        partnerRepository.delete(partner);
    }

    @Transactional(readOnly = true)
    public Partner findEntity(Long id) {
        return partnerRepository.findById(id).orElseThrow(() -> new NotFoundException("Partner not found"));
    }

    private void apply(Partner partner, String name, boolean displayOnWaiting, MultipartFile logo, boolean logoRequired) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isEmpty() || normalizedName.length() > 120) {
            throw new BadRequestException("Le nom du partenaire est obligatoire et limité à 120 caractères.");
        }
        partner.setName(normalizedName);
        partner.setDisplayOnWaiting(displayOnWaiting);

        if (logo == null || logo.isEmpty()) {
            if (logoRequired) throw new BadRequestException("Le logo du partenaire est obligatoire.");
            return;
        }
        if (logo.getSize() > MAX_LOGO_SIZE || !ALLOWED_TYPES.contains(logo.getContentType())) {
            throw new BadRequestException("Le logo doit être une image PNG, JPEG ou WebP de 2 Mo maximum.");
        }
        try {
            partner.setLogoData(logo.getBytes());
            partner.setLogoContentType(logo.getContentType());
        } catch (IOException exception) {
            throw new BadRequestException("Le logo n'a pas pu être lu.");
        }
    }

    private PartnerResponse toResponse(Partner partner) {
        long version = partner.getUpdatedAt() == null ? 0 : partner.getUpdatedAt().toEpochMilli();
        return new PartnerResponse(partner.getId(), partner.getName(), partner.isDisplayOnWaiting(),
                "/api/partners/" + partner.getId() + "/logo?v=" + version);
    }
}
