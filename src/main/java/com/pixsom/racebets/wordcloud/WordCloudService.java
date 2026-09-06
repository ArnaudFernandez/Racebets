package com.pixsom.racebets.wordcloud;

import com.pixsom.racebets.admin.ConflictException;
import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.app.AppFeatureSettingsService;
import com.pixsom.racebets.app.AppMode;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.repositories.AppUserRepository;
import com.pixsom.racebets.repositories.WordCloudModeratedWordRepository;
import com.pixsom.racebets.repositories.WordCloudQuestionRepository;
import com.pixsom.racebets.repositories.WordCloudResponseRepository;
import com.pixsom.racebets.wordcloud.dto.WordCloudAdminSnapshotResponse;
import com.pixsom.racebets.wordcloud.dto.WordCloudAdminWordResponse;
import com.pixsom.racebets.wordcloud.dto.WordCloudModerationRequest;
import com.pixsom.racebets.wordcloud.dto.WordCloudQuestionListResponse;
import com.pixsom.racebets.wordcloud.dto.WordCloudQuestionRequest;
import com.pixsom.racebets.wordcloud.dto.WordCloudSnapshotResponse;
import com.pixsom.racebets.wordcloud.dto.WordCloudWordResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WordCloudService {

    private static final String ACTIVE_QUESTION_CONFLICT =
            "A word cloud question is already active. Close it before opening another one.";

    private final WordCloudQuestionRepository questionRepository;
    private final WordCloudResponseRepository responseRepository;
    private final WordCloudModeratedWordRepository moderatedWordRepository;
    private final AppUserRepository appUserRepository;
    private final AppFeatureSettingsService featureSettingsService;

    public WordCloudService(
            WordCloudQuestionRepository questionRepository,
            WordCloudResponseRepository responseRepository,
            WordCloudModeratedWordRepository moderatedWordRepository,
            AppUserRepository appUserRepository,
            AppFeatureSettingsService featureSettingsService
    ) {
        this.questionRepository = questionRepository;
        this.responseRepository = responseRepository;
        this.moderatedWordRepository = moderatedWordRepository;
        this.appUserRepository = appUserRepository;
        this.featureSettingsService = featureSettingsService;
    }

    @Transactional(readOnly = true)
    public List<WordCloudQuestionListResponse> findQuestions() {
        return questionRepository.findAll(Sort.by("createdAt").descending())
                .stream()
                .map(this::toListResponse)
                .toList();
    }

    @Transactional
    public WordCloudQuestionListResponse createQuestion(WordCloudQuestionRequest request) {
        WordCloudQuestion question = new WordCloudQuestion();
        question.setText(safeDisplayText(request.text()));
        return toListResponse(questionRepository.save(question));
    }

    @Transactional
    public WordCloudQuestionListResponse updateQuestion(Long id, WordCloudQuestionRequest request) {
        WordCloudQuestion question = findQuestionForUpdate(id);
        requireDraft(question, "Only a draft word cloud question can be edited");
        question.setText(safeDisplayText(request.text()));
        return toListResponse(questionRepository.save(question));
    }

    @Transactional
    public void deleteQuestion(Long id) {
        WordCloudQuestion question = findQuestionForUpdate(id);
        requireDraft(question, "Only a draft word cloud question can be deleted");
        questionRepository.delete(question);
    }

    @Transactional(readOnly = true)
    public WordCloudAdminSnapshotResponse findAdminQuestion(Long id) {
        WordCloudQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Word cloud question not found"));
        return toAdminSnapshot(question);
    }

    @Transactional
    public WordCloudSnapshotResponse openQuestion(Long id) {
        featureSettingsService.activateMode(AppMode.WORD_CLOUD);
        WordCloudQuestion question = findQuestionForUpdate(id);
        requireDraft(question, "A previously posed word cloud question cannot be opened again");
        if (questionRepository.existsByActiveSlotTrue()) {
            throw new ConflictException(ACTIVE_QUESTION_CONFLICT);
        }

        question.setStatus(WordCloudQuestionStatus.OPEN);
        question.setOpenedAt(Instant.now());
        question.setActiveSlot(true);
        try {
            return toSnapshot(questionRepository.saveAndFlush(question), null, true);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(ACTIVE_QUESTION_CONFLICT);
        }
    }

    @Transactional
    public WordCloudSnapshotResponse revealQuestion(Long id) {
        WordCloudQuestion question = findQuestionForUpdate(id);
        requireStatus(question, WordCloudQuestionStatus.OPEN);
        question.setStatus(WordCloudQuestionStatus.REVEALED);
        question.setRevealedAt(Instant.now());
        return toSnapshot(question, null, true);
    }

    @Transactional
    public WordCloudSnapshotResponse closeQuestion(Long id) {
        WordCloudQuestion question = findQuestionForUpdate(id);
        if (question.getStatus() != WordCloudQuestionStatus.OPEN
                && question.getStatus() != WordCloudQuestionStatus.REVEALED) {
            throw new ConflictException("Only an open or revealed word cloud question can be closed");
        }
        question.setStatus(WordCloudQuestionStatus.CLOSED);
        question.setClosedAt(Instant.now());
        question.setActiveSlot(null);
        return toSnapshot(question, null, true);
    }

    @Transactional
    public WordCloudAdminSnapshotResponse resetQuestion(Long id) {
        WordCloudQuestion question = findQuestionForUpdate(id);
        requireStatus(question, WordCloudQuestionStatus.CLOSED);

        responseRepository.deleteByQuestion(question);
        moderatedWordRepository.deleteByQuestion(question);
        question.setStatus(WordCloudQuestionStatus.DRAFT);
        question.setActiveSlot(null);
        question.setOpenedAt(null);
        question.setRevealedAt(null);
        question.setClosedAt(null);
        questionRepository.save(question);
        return toAdminSnapshot(question);
    }

    @Transactional
    public WordCloudAdminSnapshotResponse censorResponse(Long id, WordCloudModerationRequest request) {
        WordCloudQuestion question = findQuestionForUpdate(id);
        if (question.getStatus() != WordCloudQuestionStatus.OPEN
                && question.getStatus() != WordCloudQuestionStatus.REVEALED) {
            throw new ConflictException("Responses can be moderated only while the question is active");
        }

        String normalizedText = normalizedText(request.text());
        if (!responseRepository.existsByQuestionAndNormalizedText(question, normalizedText)) {
            throw new NotFoundException("Word cloud response not found");
        }
        if (!moderatedWordRepository.existsByQuestionAndNormalizedText(question, normalizedText)) {
            WordCloudModeratedWord moderatedWord = new WordCloudModeratedWord();
            moderatedWord.setQuestion(question);
            moderatedWord.setNormalizedText(normalizedText);
            moderatedWordRepository.save(moderatedWord);
        }
        return toAdminSnapshot(question);
    }

    @Transactional(readOnly = true)
    public Optional<WordCloudSnapshotResponse> findAdminLive() {
        return findLiveQuestion().map(question -> toSnapshot(question, null, true));
    }

    @Transactional(readOnly = true)
    public Optional<WordCloudSnapshotResponse> findPublicLive() {
        return findLiveQuestion().map(question -> toSnapshot(question, null, true));
    }

    @Transactional
    public Optional<WordCloudSnapshotResponse> findPlayerLive(Long userId) {
        featureSettingsService.requireActiveMode(AppMode.WORD_CLOUD);
        return findLiveQuestion().map(question -> toSnapshot(question, findUser(userId), false));
    }

    @Transactional
    public WordCloudSnapshotResponse submitResponse(Long questionId, String text, Long userId) {
        featureSettingsService.requireActiveMode(AppMode.WORD_CLOUD);
        WordCloudQuestion question = findQuestionForUpdate(questionId);
        if (question.getStatus() != WordCloudQuestionStatus.OPEN || !Boolean.TRUE.equals(question.getActiveSlot())) {
            throw new ConflictException("Responses are accepted only for the active open word cloud question");
        }

        AppUser user = findUser(userId);
        WordCloudResponse response = responseRepository.findByQuestionAndUser(question, user)
                .orElseGet(() -> {
                    WordCloudResponse created = new WordCloudResponse();
                    created.setQuestion(question);
                    created.setUser(user);
                    return created;
                });
        String displayText = safeDisplayText(text);
        response.setDisplayText(displayText);
        response.setNormalizedText(normalizedText(displayText));
        responseRepository.save(response);
        return toSnapshot(question, user, false);
    }

    static String normalizedText(String text) {
        return Normalizer.normalize(safeDisplayText(text), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.FRENCH);
    }

    private static String safeDisplayText(String text) {
        return text.replaceAll("(?U)\\s+", " ").strip();
    }

    private Optional<WordCloudQuestion> findLiveQuestion() {
        return questionRepository.findByActiveSlotTrue()
                .filter(question -> question.getStatus() == WordCloudQuestionStatus.OPEN
                        || question.getStatus() == WordCloudQuestionStatus.REVEALED);
    }

    private WordCloudQuestion findQuestionForUpdate(Long id) {
        return questionRepository.findLockedById(id)
                .orElseThrow(() -> new NotFoundException("Word cloud question not found"));
    }

    private AppUser findUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }

    private void requireDraft(WordCloudQuestion question, String message) {
        if (question.getStatus() != WordCloudQuestionStatus.DRAFT) {
            throw new ConflictException(message);
        }
    }

    private void requireStatus(WordCloudQuestion question, WordCloudQuestionStatus expected) {
        if (question.getStatus() != expected) {
            throw new ConflictException("Invalid word cloud question status: expected " + expected
                    + " but was " + question.getStatus());
        }
    }

    private WordCloudQuestionListResponse toListResponse(WordCloudQuestion question) {
        return new WordCloudQuestionListResponse(
                question.getId(),
                question.getText(),
                question.getStatus(),
                responseRepository.countByQuestion(question),
                question.getCreatedAt(),
                question.getOpenedAt(),
                question.getRevealedAt(),
                question.getClosedAt()
        );
    }

    private WordCloudSnapshotResponse toSnapshot(WordCloudQuestion question, AppUser user, boolean adminView) {
        List<WordCloudResponse> responses = responseRepository.findByQuestionOrderByCreatedAtAscIdAsc(question);
        String currentUserResponse = user == null ? null : responses.stream()
                .filter(response -> response.getUser().getId().equals(user.getId()))
                .map(WordCloudResponse::getDisplayText)
                .findFirst()
                .orElse(null);
        boolean showWords = adminView || question.getStatus() == WordCloudQuestionStatus.REVEALED;
        Set<String> censoredWords = showWords ? censoredWords(question) : Set.of();
        return new WordCloudSnapshotResponse(
                question.getId(),
                question.getText(),
                question.getStatus(),
                question.getOpenedAt(),
                responses.size(),
                currentUserResponse,
                showWords ? aggregate(responses, censoredWords) : List.of()
        );
    }

    private WordCloudAdminSnapshotResponse toAdminSnapshot(WordCloudQuestion question) {
        List<WordCloudResponse> responses = responseRepository.findByQuestionOrderByCreatedAtAscIdAsc(question);
        Set<String> censoredWords = censoredWords(question);
        Map<String, WordCount> counts = countsByNormalizedText(responses);
        List<WordCloudAdminWordResponse> adminResponses = counts.entrySet().stream()
                .map(entry -> new WordCloudAdminWordResponse(
                        entry.getValue().text(),
                        entry.getValue().count(),
                        censoredWords.contains(entry.getKey())
                ))
                .sorted(Comparator.comparingLong(WordCloudAdminWordResponse::count).reversed()
                        .thenComparing(WordCloudAdminWordResponse::text))
                .toList();
        return new WordCloudAdminSnapshotResponse(
                question.getId(),
                question.getText(),
                question.getStatus(),
                question.getCreatedAt(),
                question.getOpenedAt(),
                question.getRevealedAt(),
                question.getClosedAt(),
                responses.size(),
                adminResponses
        );
    }

    private Set<String> censoredWords(WordCloudQuestion question) {
        return moderatedWordRepository.findByQuestion(question).stream()
                .map(WordCloudModeratedWord::getNormalizedText)
                .collect(Collectors.toUnmodifiableSet());
    }

    private List<WordCloudWordResponse> aggregate(List<WordCloudResponse> responses, Set<String> censoredWords) {
        Map<String, WordCount> counts = countsByNormalizedText(responses);
        List<WordCloudWordResponse> words = counts.entrySet().stream()
                .filter(entry -> !censoredWords.contains(entry.getKey()))
                .map(entry -> new WordCloudWordResponse(entry.getValue().text(), entry.getValue().count()))
                .sorted(Comparator.comparingLong(WordCloudWordResponse::count).reversed()
                        .thenComparing(WordCloudWordResponse::text))
                .toList();
        return List.copyOf(words);
    }

    private Map<String, WordCount> countsByNormalizedText(List<WordCloudResponse> responses) {
        Map<String, WordCount> counts = new LinkedHashMap<>();
        for (WordCloudResponse response : responses) {
            counts.compute(response.getNormalizedText(), (key, count) -> count == null
                    ? new WordCount(response.getDisplayText(), 1)
                    : new WordCount(count.text(), count.count() + 1));
        }
        return counts;
    }

    private record WordCount(String text, long count) {
    }
}
