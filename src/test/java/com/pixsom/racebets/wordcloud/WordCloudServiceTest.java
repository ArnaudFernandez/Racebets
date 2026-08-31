package com.pixsom.racebets.wordcloud;

import com.pixsom.racebets.admin.ConflictException;
import com.pixsom.racebets.app.AppFeatureSettingsService;
import com.pixsom.racebets.app.AppMode;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.repositories.AppUserRepository;
import com.pixsom.racebets.repositories.WordCloudModeratedWordRepository;
import com.pixsom.racebets.repositories.WordCloudQuestionRepository;
import com.pixsom.racebets.repositories.WordCloudResponseRepository;
import com.pixsom.racebets.wordcloud.dto.WordCloudQuestionRequest;
import com.pixsom.racebets.wordcloud.dto.WordCloudModerationRequest;
import com.pixsom.racebets.wordcloud.dto.WordCloudSnapshotResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WordCloudServiceTest {

    @Mock WordCloudQuestionRepository questionRepository;
    @Mock WordCloudResponseRepository responseRepository;
    @Mock WordCloudModeratedWordRepository moderatedWordRepository;
    @Mock AppUserRepository appUserRepository;
    @Mock AppFeatureSettingsService featureSettingsService;

    private WordCloudService service;

    @BeforeEach
    void setUp() {
        service = new WordCloudService(
                questionRepository, responseRepository, moderatedWordRepository, appUserRepository, featureSettingsService);
    }

    @Test
    void questionFollowsOpenRevealCloseLifecycleAndCannotReopen() {
        WordCloudQuestion question = question(1L, WordCloudQuestionStatus.DRAFT);
        when(questionRepository.findLockedById(1L)).thenReturn(Optional.of(question));
        when(questionRepository.existsByActiveSlotTrue()).thenReturn(false);
        when(questionRepository.saveAndFlush(question)).thenReturn(question);

        assertThat(service.openQuestion(1L).status()).isEqualTo(WordCloudQuestionStatus.OPEN);
        assertThat(question.getOpenedAt()).isNotNull();
        assertThat(question.getActiveSlot()).isTrue();

        assertThat(service.revealQuestion(1L).status()).isEqualTo(WordCloudQuestionStatus.REVEALED);
        assertThat(question.getRevealedAt()).isNotNull();
        assertThat(question.getActiveSlot()).isTrue();

        assertThat(service.closeQuestion(1L).status()).isEqualTo(WordCloudQuestionStatus.CLOSED);
        assertThat(question.getClosedAt()).isNotNull();
        assertThat(question.getActiveSlot()).isNull();
        assertThatThrownBy(() -> service.openQuestion(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cannot be opened again");
    }

    @Test
    void secondActiveQuestionIsRejectedBeforeSave() {
        WordCloudQuestion question = question(2L, WordCloudQuestionStatus.DRAFT);
        when(questionRepository.findLockedById(2L)).thenReturn(Optional.of(question));
        when(questionRepository.existsByActiveSlotTrue()).thenReturn(true);

        assertThatThrownBy(() -> service.openQuestion(2L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already active");
        verify(questionRepository, never()).saveAndFlush(any());
    }

    @Test
    void inactiveWordCloudModeRejectsOpeningBeforeQuestionLock() {
        org.mockito.Mockito.doThrow(new ConflictException("Application mode WORD_CLOUD is not active"))
                .when(featureSettingsService).requireActiveMode(AppMode.WORD_CLOUD);

        assertThatThrownBy(() -> service.openQuestion(2L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not active");
        verify(questionRepository, never()).findLockedById(any());
    }

    @Test
    void databaseRaceWhenOpeningIsTranslatedToConflict() {
        WordCloudQuestion question = question(3L, WordCloudQuestionStatus.DRAFT);
        when(questionRepository.findLockedById(3L)).thenReturn(Optional.of(question));
        when(questionRepository.existsByActiveSlotTrue()).thenReturn(false);
        when(questionRepository.saveAndFlush(question)).thenThrow(new DataIntegrityViolationException("constraint details"));

        assertThatThrownBy(() -> service.openQuestion(3L))
                .isInstanceOf(ConflictException.class)
                .hasMessageNotContaining("constraint details");
    }

    @Test
    void onlyDraftQuestionsCanBeEditedOrDeleted() {
        WordCloudQuestion draft = question(4L, WordCloudQuestionStatus.DRAFT);
        WordCloudQuestion open = question(5L, WordCloudQuestionStatus.OPEN);
        when(questionRepository.findLockedById(4L)).thenReturn(Optional.of(draft));
        when(questionRepository.findLockedById(5L)).thenReturn(Optional.of(open));
        when(questionRepository.save(draft)).thenReturn(draft);

        service.updateQuestion(4L, new WordCloudQuestionRequest("  Nouveau   texte "));
        service.deleteQuestion(4L);

        assertThat(draft.getText()).isEqualTo("Nouveau texte");
        verify(questionRepository).delete(draft);
        assertThatThrownBy(() -> service.updateQuestion(5L, new WordCloudQuestionRequest("Interdit")))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.deleteQuestion(5L))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void responseIsUpsertedForJwtUserWhileQuestionIsLocked() {
        WordCloudQuestion question = question(6L, WordCloudQuestionStatus.OPEN);
        question.setActiveSlot(true);
        AppUser user = user(10L);
        WordCloudResponse existing = response(question, user, "Ancien", "ancien");
        when(questionRepository.findLockedById(6L)).thenReturn(Optional.of(question));
        when(appUserRepository.findById(10L)).thenReturn(Optional.of(user));
        when(responseRepository.findByQuestionAndUser(question, user)).thenReturn(Optional.of(existing));
        when(responseRepository.findByQuestionOrderByCreatedAtAscIdAsc(question)).thenReturn(List.of(existing));

        var snapshot = service.submitResponse(6L, "  ÉCLAIR   blanc ", 10L);

        verify(featureSettingsService).requireActiveMode(AppMode.WORD_CLOUD);
        assertThat(existing.getDisplayText()).isEqualTo("ÉCLAIR blanc");
        assertThat(existing.getNormalizedText()).isEqualTo("eclair blanc");
        assertThat(snapshot.currentUserResponse()).isEqualTo("ÉCLAIR blanc");
        verify(questionRepository).findLockedById(6L);
        verify(responseRepository).save(existing);
    }

    @Test
    void inactiveWordCloudModeRejectsResponseBeforeQuestionLock() {
        org.mockito.Mockito.doThrow(new ConflictException("Application mode WORD_CLOUD is not active"))
                .when(featureSettingsService).requireActiveMode(AppMode.WORD_CLOUD);

        assertThatThrownBy(() -> service.submitResponse(6L, "Mot", 10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not active");
        verify(questionRepository, never()).findLockedById(any());
    }

    @Test
    void responseOutsideActiveOpenQuestionIsRejected() {
        WordCloudQuestion question = question(7L, WordCloudQuestionStatus.REVEALED);
        question.setActiveSlot(true);
        when(questionRepository.findLockedById(7L)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> service.submitResponse(7L, "Mot", 10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("active open");
        verify(appUserRepository, never()).findById(any());
        verify(responseRepository, never()).save(any());
    }

    @Test
    void aggregatesAreHiddenUntilRevealAndContainNoParticipantIdentity() {
        WordCloudQuestion question = question(8L, WordCloudQuestionStatus.OPEN);
        question.setActiveSlot(true);
        AppUser firstUser = user(11L);
        AppUser currentUser = user(12L);
        AppUser thirdUser = user(13L);
        AppUser fourthUser = user(14L);
        List<WordCloudResponse> responses = List.of(
                response(question, firstUser, "Éclair", "eclair"),
                response(question, currentUser, "éclair", "eclair"),
                response(question, thirdUser, "zulu", "zulu"),
                response(question, fourthUser, "alpha", "alpha")
        );
        when(questionRepository.findByActiveSlotTrue()).thenReturn(Optional.of(question));
        when(appUserRepository.findById(12L)).thenReturn(Optional.of(currentUser));
        when(responseRepository.findByQuestionOrderByCreatedAtAscIdAsc(question)).thenReturn(responses);

        WordCloudSnapshotResponse openSnapshot = service.findPlayerLive(12L).orElseThrow();
        assertThat(openSnapshot.submissionCount()).isEqualTo(4);
        assertThat(openSnapshot.currentUserResponse()).isEqualTo("éclair");
        assertThat(openSnapshot.words()).isEmpty();

        question.setStatus(WordCloudQuestionStatus.REVEALED);
        WordCloudSnapshotResponse revealedSnapshot = service.findPlayerLive(12L).orElseThrow();
        assertThat(revealedSnapshot.words())
                .extracting(word -> word.text() + ":" + word.count())
                .containsExactly("Éclair:2", "alpha:1", "zulu:1");
        assertThat(WordCloudSnapshotResponse.class.getRecordComponents())
                .extracting(component -> component.getName())
                .doesNotContain("userId", "participants", "responses");
    }

    @Test
    void adminSeesAggregatesBeforeRevealAndNoLiveUsesOptional() {
        WordCloudQuestion question = question(9L, WordCloudQuestionStatus.OPEN);
        question.setActiveSlot(true);
        AppUser user = user(15L);
        when(questionRepository.findByActiveSlotTrue())
                .thenReturn(Optional.of(question), Optional.empty());
        when(responseRepository.findByQuestionOrderByCreatedAtAscIdAsc(question))
                .thenReturn(List.of(response(question, user, "Paris", "paris")));

        assertThat(service.findAdminLive().orElseThrow().words()).hasSize(1);
        assertThat(service.findAdminLive()).isEmpty();
    }

    @Test
    void closedQuestionCanBeResetAndLosesResponsesAndModeration() {
        WordCloudQuestion question = question(10L, WordCloudQuestionStatus.CLOSED);
        question.setOpenedAt(java.time.Instant.now());
        question.setRevealedAt(java.time.Instant.now());
        question.setClosedAt(java.time.Instant.now());
        when(questionRepository.findLockedById(10L)).thenReturn(Optional.of(question));

        var snapshot = service.resetQuestion(10L);

        assertThat(snapshot.status()).isEqualTo(WordCloudQuestionStatus.DRAFT);
        assertThat(snapshot.submissionCount()).isZero();
        assertThat(question.getOpenedAt()).isNull();
        assertThat(question.getRevealedAt()).isNull();
        assertThat(question.getClosedAt()).isNull();
        verify(responseRepository).deleteByQuestion(question);
        verify(moderatedWordRepository).deleteByQuestion(question);
        verify(questionRepository).save(question);
    }

    @Test
    void censoredNormalizedResponseIsExcludedFromPlayerCloud() {
        WordCloudQuestion question = question(11L, WordCloudQuestionStatus.OPEN);
        question.setActiveSlot(true);
        AppUser user = user(16L);
        WordCloudResponse response = response(question, user, "Énergie", "energie");
        WordCloudModeratedWord moderatedWord = new WordCloudModeratedWord();
        moderatedWord.setQuestion(question);
        moderatedWord.setNormalizedText("energie");
        when(questionRepository.findLockedById(11L)).thenReturn(Optional.of(question));
        when(responseRepository.existsByQuestionAndNormalizedText(question, "energie")).thenReturn(true);
        when(responseRepository.findByQuestionOrderByCreatedAtAscIdAsc(question)).thenReturn(List.of(response));
        when(moderatedWordRepository.findByQuestion(question)).thenReturn(List.of(moderatedWord));

        var adminSnapshot = service.censorResponse(11L, new WordCloudModerationRequest("Energie"));

        assertThat(adminSnapshot.responses()).singleElement().satisfies(group -> {
            assertThat(group.text()).isEqualTo("Énergie");
            assertThat(group.censored()).isTrue();
        });
        question.setStatus(WordCloudQuestionStatus.REVEALED);
        when(questionRepository.findByActiveSlotTrue()).thenReturn(Optional.of(question));
        when(appUserRepository.findById(16L)).thenReturn(Optional.of(user));
        assertThat(service.findPlayerLive(16L).orElseThrow().words()).isEmpty();
        verify(moderatedWordRepository).save(any(WordCloudModeratedWord.class));
    }

    private WordCloudQuestion question(Long id, WordCloudQuestionStatus status) {
        WordCloudQuestion question = new WordCloudQuestion();
        ReflectionTestUtils.setField(question, "id", id);
        question.setText("Votre mot ?");
        question.setStatus(status);
        return question;
    }

    private AppUser user(Long id) {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private WordCloudResponse response(
            WordCloudQuestion question,
            AppUser user,
            String displayText,
            String normalizedText
    ) {
        WordCloudResponse response = new WordCloudResponse();
        response.setQuestion(question);
        response.setUser(user);
        response.setDisplayText(displayText);
        response.setNormalizedText(normalizedText);
        return response;
    }
}
