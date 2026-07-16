package com.pixsom.racebets.partner;

import com.pixsom.racebets.admin.BadRequestException;
import com.pixsom.racebets.entities.Partner;
import com.pixsom.racebets.repositories.PartnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerServiceTest {

    @Mock PartnerRepository partnerRepository;
    private PartnerService service;

    @BeforeEach
    void setUp() {
        service = new PartnerService(partnerRepository);
    }

    @Test
    void createsPartnerWithValidatedLogo() {
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", new byte[]{1, 2, 3});
        when(partnerRepository.save(any(Partner.class))).thenAnswer(invocation -> {
            Partner partner = invocation.getArgument(0);
            ReflectionTestUtils.setField(partner, "id", 7L);
            return partner;
        });

        var response = service.create("  Hippodrome partenaire  ", true, logo);

        ArgumentCaptor<Partner> captor = ArgumentCaptor.forClass(Partner.class);
        verify(partnerRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Hippodrome partenaire");
        assertThat(captor.getValue().getLogoData()).containsExactly(1, 2, 3);
        assertThat(captor.getValue().isDisplayOnWaiting()).isTrue();
        assertThat(response.logoUrl()).isEqualTo("/api/partners/7/logo?v=0");
    }

    @Test
    void rejectsUnsupportedLogo() {
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.svg", "image/svg+xml", new byte[]{1});

        assertThatThrownBy(() -> service.create("Partenaire", true, logo))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PNG, JPEG ou WebP");
    }

    @Test
    void updateKeepsExistingLogoWhenNoFileIsProvided() {
        Partner partner = partner(3L, "Ancien nom", true);
        byte[] existingLogo = new byte[]{4, 5};
        partner.setLogoData(existingLogo);
        partner.setLogoContentType("image/png");
        when(partnerRepository.findById(3L)).thenReturn(Optional.of(partner));
        when(partnerRepository.save(partner)).thenReturn(partner);

        service.update(3L, "Nouveau nom", false, null);

        assertThat(partner.getName()).isEqualTo("Nouveau nom");
        assertThat(partner.isDisplayOnWaiting()).isFalse();
        assertThat(partner.getLogoData()).isSameAs(existingLogo);
    }

    @Test
    void publicListContainsOnlyVisiblePartnersOrderedById() {
        when(partnerRepository.findAllByDisplayOnWaitingTrue(Sort.by("id").ascending()))
                .thenReturn(List.of(partner(2L, "Visible", true)));

        assertThat(service.findVisible()).extracting(item -> item.name()).containsExactly("Visible");
    }

    private Partner partner(Long id, String name, boolean visible) {
        Partner partner = new Partner();
        ReflectionTestUtils.setField(partner, "id", id);
        partner.setName(name);
        partner.setDisplayOnWaiting(visible);
        partner.setLogoData(new byte[]{1});
        partner.setLogoContentType("image/png");
        return partner;
    }
}
