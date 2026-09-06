package com.pixsom.racebets.admin.user;

import com.pixsom.racebets.admin.BadRequestException;
import com.pixsom.racebets.admin.ConflictException;
import com.pixsom.racebets.admin.user.dto.UserImportAction;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.enums.Role;
import com.pixsom.racebets.repositories.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserImportServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Test
    void previewReadsWindows1252AndSkipsSectionRows() {
        AppUser unchanged = user("lea@example.com", "Léa", "PEYRAN", true, Role.USER);
        AppUser updated = user("admin@example.com", "Ancien", "Nom", false, Role.ADMIN);
        when(userRepository.findAll()).thenReturn(List.of(unchanged, updated));
        String csv = "Email professionnel;Prénom;Nom;;;;;\n"
                + "Collaborateurs;;;;;;;\n"
                + "gregory@example.com;Grégory;JOBLIN;;;;;\n"
                + "lea@example.com;Léa;PEYRAN;;;;;\n"
                + "admin@example.com;Alice;ADMIN;;;;;note\n"
                + "Partenaires :;;;;;;;\n";

        var preview = new UserImportService(userRepository).preview(file(
                csv.getBytes(Charset.forName("windows-1252"))));

        assertThat(preview.totalRows()).isEqualTo(3);
        assertThat(preview.createCount()).isEqualTo(1);
        assertThat(preview.updateCount()).isEqualTo(1);
        assertThat(preview.unchangedCount()).isEqualTo(1);
        assertThat(preview.errorCount()).isZero();
        assertThat(preview.importable()).isTrue();
        assertThat(preview.rows().getFirst().name()).isEqualTo("Grégory");
        assertThat(preview.rows().get(2).warning()).contains("supplémentaires");
    }

    @Test
    void previewRejectsDuplicateNormalizedEmailsWithoutWriting() {
        when(userRepository.findAll()).thenReturn(List.of());
        String csv = "Email professionnel;Prénom;Nom\n"
                + "CAMILLE@example.com;Camille;Martin\n"
                + " camille@example.com ;Camille;Martin\n";

        var preview = new UserImportService(userRepository).preview(file(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(preview.errorCount()).isEqualTo(1);
        assertThat(preview.importable()).isFalse();
        assertThat(preview.rows().get(1).action()).isEqualTo(UserImportAction.ERROR);
        assertThat(preview.rows().get(1).error()).contains("plusieurs fois");
        verify(userRepository, never()).saveAll(any());
    }

    @Test
    void confirmCreatesUsersAndPreservesExistingSecurityData() {
        AppUser existing = user("member@example.com", "Ancien", "Nom", false, Role.VIP);
        existing.setPasswordHash("existing-password-hash");
        when(userRepository.findAll()).thenReturn(List.of(existing));
        when(userRepository.findAllForUpdate()).thenReturn(List.of(existing));
        String csv = "Email professionnel;Prénom;Nom\n"
                + "member@example.com;Nouveau;Nom\n"
                + "new@example.com;Nouvelle;Personne\n";
        MockMultipartFile file = file(csv.getBytes(StandardCharsets.UTF_8));
        UserImportService service = new UserImportService(userRepository);
        var preview = service.preview(file);

        var result = service.confirm(file, preview.fileDigest(), preview.planFingerprint());

        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.updatedCount()).isEqualTo(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<AppUser>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(userRepository).saveAll(captor.capture());
        List<AppUser> saved = ((List<AppUser>) captor.getValue());
        assertThat(saved).hasSize(2);
        assertThat(existing.getName()).isEqualTo("Nouveau");
        assertThat(existing.isPresent()).isTrue();
        assertThat(existing.getRoles()).containsExactly(Role.VIP);
        assertThat(existing.getPasswordHash()).isEqualTo("existing-password-hash");
        AppUser created = saved.stream().filter(user -> user.getEmail().equals("new@example.com")).findFirst().orElseThrow();
        assertThat(created.getRoles()).containsExactly(Role.USER);
        assertThat(created.getPasswordHash()).isNull();
        assertThat(created.isPresent()).isTrue();
    }

    @Test
    void confirmRejectsAPlanWhenAccountsChangedAfterPreview() {
        String csv = "Email professionnel;Prénom;Nom\nnew@example.com;Nouvelle;Personne\n";
        MockMultipartFile file = file(csv.getBytes(StandardCharsets.UTF_8));
        AppUser concurrentlyCreated = user("new@example.com", "Autre", "Personne", true, Role.USER);
        when(userRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAllForUpdate()).thenReturn(List.of(concurrentlyCreated));
        UserImportService service = new UserImportService(userRepository);
        var preview = service.preview(file);

        assertThatThrownBy(() -> service.confirm(file, preview.fileDigest(), preview.planFingerprint()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("prévisualisation");

        verify(userRepository, never()).saveAll(any());
    }

    @Test
    void previewRejectsAnEmptyCsv() {
        when(userRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> new UserImportService(userRepository).preview(file(
                "Email professionnel;Prénom;Nom\n".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Aucune personne");
    }

    @Test
    void previewRejectsAnInvalidFileTypeOrHeader() {
        UserImportService service = new UserImportService(userRepository);

        assertThatThrownBy(() -> service.preview(new MockMultipartFile(
                "file", "participants.txt", "text/plain", "data".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("format CSV");
        assertThatThrownBy(() -> service.preview(file(
                "Courriel;Prenom;Nom\nmember@example.com;Member;Test\n".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("doit contenir les colonnes");
    }

    private AppUser user(String email, String name, String surname, boolean present, Role role) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setName(name);
        user.setSurname(surname);
        user.setPresent(present);
        user.setRoles(Set.of(role));
        return user;
    }

    private MockMultipartFile file(byte[] content) {
        return new MockMultipartFile("file", "participants.csv", "text/csv", content);
    }
}
