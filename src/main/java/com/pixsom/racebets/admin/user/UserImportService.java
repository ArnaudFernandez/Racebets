package com.pixsom.racebets.admin.user;

import com.pixsom.racebets.admin.BadRequestException;
import com.pixsom.racebets.admin.ConflictException;
import com.pixsom.racebets.admin.user.dto.UserImportAction;
import com.pixsom.racebets.admin.user.dto.UserImportPreviewResponse;
import com.pixsom.racebets.admin.user.dto.UserImportResultResponse;
import com.pixsom.racebets.admin.user.dto.UserImportRowResponse;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.enums.Role;
import com.pixsom.racebets.repositories.AppUserRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class UserImportService {

    private static final long MAX_FILE_SIZE = 1024 * 1024;
    private static final int MAX_ROWS = 2_000;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .get();

    private final AppUserRepository userRepository;

    public UserImportService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserImportPreviewResponse preview(MultipartFile file) {
        return buildPlan(readFile(file), usersByNormalizedEmail(userRepository.findAll()));
    }

    @Transactional
    public UserImportResultResponse confirm(MultipartFile file, String expectedFileDigest,
                                            String expectedPlanFingerprint) {
        ImportFile importFile = readFile(file);
        Map<String, AppUser> existingUsers = usersByNormalizedEmail(userRepository.findAllForUpdate());
        UserImportPreviewResponse plan = buildPlan(importFile, existingUsers);
        if (!plan.fileDigest().equals(expectedFileDigest)
                || !plan.planFingerprint().equals(expectedPlanFingerprint)) {
            throw new ConflictException("Le fichier ou les comptes ont changé depuis la prévisualisation. Analysez à nouveau le CSV.");
        }
        if (!plan.importable()) {
            throw new BadRequestException("Le CSV contient des erreurs et ne peut pas être importé.");
        }

        List<AppUser> changedUsers = new ArrayList<>();
        for (UserImportRowResponse row : plan.rows()) {
            if (row.action() == UserImportAction.UNCHANGED) {
                continue;
            }
            AppUser user = existingUsers.get(row.email());
            if (user == null) {
                user = new AppUser();
                user.setRoles(Set.of(Role.USER));
                user.setTutorialCompleted(false);
            }
            user.setEmail(row.email());
            user.setName(row.name());
            user.setSurname(row.surname());
            user.setPresent(true);
            changedUsers.add(user);
        }
        try {
            userRepository.saveAll(changedUsers);
            userRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Un compte utilise désormais une adresse email du CSV. Analysez à nouveau le fichier.");
        }
        return new UserImportResultResponse(plan.createCount(), plan.updateCount(), plan.unchangedCount());
    }

    private UserImportPreviewResponse buildPlan(ImportFile file, Map<String, AppUser> existingUsers) {
        List<List<String>> records = parse(file.content());
        if (records.isEmpty()) {
            throw new BadRequestException("Le fichier CSV est vide.");
        }
        Header header = findHeader(records.getFirst());
        Set<String> importedEmails = new HashSet<>();
        List<UserImportRowResponse> rows = new ArrayList<>();

        for (int index = 1; index < records.size(); index++) {
            List<String> record = records.get(index);
            String rawEmail = value(record, header.emailIndex());
            String name = value(record, header.nameIndex()).trim();
            String surname = value(record, header.surnameIndex()).trim();
            if (rawEmail.isBlank() && name.isBlank() && surname.isBlank()) {
                continue;
            }
            if (name.isBlank() && surname.isBlank() && !rawEmail.contains("@")) {
                continue;
            }
            if (rows.size() >= MAX_ROWS) {
                throw new BadRequestException("Le CSV ne peut pas contenir plus de 2000 personnes.");
            }

            String email = normalizeEmail(rawEmail);
            String error = validate(email, name, surname, importedEmails);
            UserImportAction action = UserImportAction.ERROR;
            if (error == null) {
                AppUser existing = existingUsers.get(email);
                if (existing == null) {
                    action = UserImportAction.CREATE;
                } else if (!name.equals(existing.getName()) || !surname.equals(existing.getSurname())
                        || !email.equals(existing.getEmail()) || !existing.isPresent()) {
                    action = UserImportAction.UPDATE;
                } else {
                    action = UserImportAction.UNCHANGED;
                }
            }
            String warning = hasExtraValues(record, header) ? "Colonnes supplémentaires ignorées." : null;
            rows.add(new UserImportRowResponse(index + 1L, email, name, surname, action, warning, error));
        }

        if (rows.isEmpty()) {
            throw new BadRequestException("Aucune personne n'a été trouvée dans le CSV.");
        }
        String fingerprint = digest(rows.stream()
                .map(row -> {
                    AppUser existing = existingUsers.get(row.email());
                    String existingState = existing == null ? "new" : existing.getId() + "|" + existing.getEmail()
                            + "|" + existing.getName() + "|" + existing.getSurname() + "|" + existing.isPresent();
                    return row.lineNumber() + "|" + row.email() + "|" + row.name() + "|" + row.surname()
                            + "|" + row.action() + "|" + row.error() + "|" + existingState;
                })
                .reduce("", (left, right) -> left + "\n" + right).getBytes(StandardCharsets.UTF_8));
        int errorCount = count(rows, UserImportAction.ERROR);
        return new UserImportPreviewResponse(
                file.digest(),
                fingerprint,
                rows.size(),
                count(rows, UserImportAction.CREATE),
                count(rows, UserImportAction.UPDATE),
                count(rows, UserImportAction.UNCHANGED),
                errorCount,
                !rows.isEmpty() && errorCount == 0,
                List.copyOf(rows)
        );
    }

    private ImportFile readFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Sélectionnez un fichier CSV.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new BadRequestException("Le fichier sélectionné doit être au format CSV.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Le fichier CSV est limité à 1 Mo.");
        }
        try {
            byte[] bytes = file.getBytes();
            String content = decode(bytes);
            if (content.indexOf('\uFFFD') >= 0) {
                throw new BadRequestException("Le fichier contient des caractères illisibles. Enregistrez-le en UTF-8 puis réessayez.");
            }
            return new ImportFile(content.replaceFirst("^\\uFEFF", ""), digest(bytes));
        } catch (IOException exception) {
            throw new BadRequestException("Le fichier CSV n'a pas pu être lu.");
        }
    }

    private String decode(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            return Charset.forName("windows-1252").decode(ByteBuffer.wrap(bytes)).toString();
        }
    }

    private List<List<String>> parse(String content) {
        try (CSVParser parser = CSV_FORMAT.parse(new StringReader(content))) {
            List<List<String>> records = new ArrayList<>();
            for (CSVRecord record : parser) {
                List<String> values = new ArrayList<>();
                record.forEach(values::add);
                records.add(values);
            }
            return records;
        } catch (IOException | IllegalArgumentException exception) {
            throw new BadRequestException("Le format du fichier CSV est invalide.");
        }
    }

    private Header findHeader(List<String> header) {
        int email = -1;
        int name = -1;
        int surname = -1;
        for (int index = 0; index < header.size(); index++) {
            String normalized = normalizeHeader(header.get(index));
            if (normalized.equals("email professionnel") || normalized.equals("email")) email = index;
            if (normalized.equals("prenom")) name = index;
            if (normalized.equals("nom")) surname = index;
        }
        if (email < 0 || name < 0 || surname < 0) {
            throw new BadRequestException("Le CSV doit contenir les colonnes Email professionnel, Prénom et Nom.");
        }
        return new Header(email, name, surname);
    }

    private String validate(String email, String name, String surname, Set<String> importedEmails) {
        if (email.contains("\uFFFD") || name.contains("\uFFFD") || surname.contains("\uFFFD")) {
            return "Caractères illisibles: enregistrez le fichier en UTF-8.";
        }
        if (email.length() > 180 || !EMAIL_PATTERN.matcher(email).matches()) return "Adresse email invalide.";
        if (name.isBlank() || name.length() > 80) return "Le prénom est obligatoire et limité à 80 caractères.";
        if (surname.isBlank() || surname.length() > 80) return "Le nom est obligatoire et limité à 80 caractères.";
        if (!importedEmails.add(email)) return "Cette adresse email apparaît plusieurs fois dans le CSV.";
        return null;
    }

    private Map<String, AppUser> usersByNormalizedEmail(List<AppUser> existingUsers) {
        Map<String, AppUser> users = new HashMap<>();
        for (AppUser user : existingUsers) {
            String normalized = normalizeEmail(user.getEmail());
            if (users.putIfAbsent(normalized, user) != null) {
                throw new ConflictException("Plusieurs comptes existants utilisent la même adresse email normalisée.");
            }
        }
        return users;
    }

    private boolean hasExtraValues(List<String> record, Header header) {
        int lastRequiredIndex = Math.max(header.emailIndex(), Math.max(header.nameIndex(), header.surnameIndex()));
        for (int index = lastRequiredIndex + 1; index < record.size(); index++) {
            if (!record.get(index).isBlank()) return true;
        }
        return false;
    }

    private String value(List<String> record, int index) {
        return index < record.size() ? record.get(index) : "";
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeHeader(String value) {
        String decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private int count(List<UserImportRowResponse> rows, UserImportAction action) {
        return (int) rows.stream().filter(row -> row.action() == action).count();
    }

    private String digest(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record Header(int emailIndex, int nameIndex, int surnameIndex) {
    }

    private record ImportFile(String content, String digest) {
    }
}
