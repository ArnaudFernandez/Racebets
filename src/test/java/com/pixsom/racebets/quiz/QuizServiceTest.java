package com.pixsom.racebets.quiz;

import com.pixsom.racebets.admin.ConflictException;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.quiz.dto.QuizAnswerRequest;
import com.pixsom.racebets.quiz.dto.QuizQuestionRequest;
import com.pixsom.racebets.quiz.dto.QuizSetRequest;
import com.pixsom.racebets.repositories.AppUserRepository;
import com.pixsom.racebets.repositories.QuizParticipantRepository;
import com.pixsom.racebets.repositories.QuizSessionRepository;
import com.pixsom.racebets.repositories.QuizSetRepository;
import com.pixsom.racebets.repositories.QuizSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock QuizSetRepository quizSetRepository;
    @Mock QuizSessionRepository quizSessionRepository;
    @Mock QuizParticipantRepository participantRepository;
    @Mock QuizSubmissionRepository submissionRepository;
    @Mock AppUserRepository appUserRepository;
    @Mock Authentication authentication;

    private QuizService service;

    @BeforeEach
    void setUp() {
        service = new QuizService(
                quizSetRepository,
                quizSessionRepository,
                participantRepository,
                submissionRepository,
                appUserRepository
        );
    }

    @Test
    void openingSessionClaimsTheUniqueActiveSlot() {
        QuizSet quizSet = quizSetWithOneQuestion();
        when(quizSetRepository.findById(1L)).thenReturn(Optional.of(quizSet));
        when(quizSessionRepository.existsByPhaseIn(any())).thenReturn(false);
        when(quizSessionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.openSession(1L);

        assertThat(response.phase()).isEqualTo(QuizSessionPhase.OPENING);
        verify(quizSessionRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(session ->
                Boolean.TRUE.equals(session.getActiveSlot()) && session.getCurrentQuestionIndex() == -1
        ));
    }

    @Test
    void openingSecondLiveSessionIsRejectedBeforeInsert() {
        QuizSet quizSet = quizSetWithOneQuestion();
        when(quizSetRepository.findById(1L)).thenReturn(Optional.of(quizSet));
        when(quizSessionRepository.existsByPhaseIn(any())).thenReturn(true);

        assertThatThrownBy(() -> service.openSession(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("session live est deja en cours");
        verify(quizSessionRepository, never()).saveAndFlush(any());
    }

    @Test
    void finishingLastQuestionReleasesTheUniqueActiveSlot() {
        QuizSession session = new QuizSession();
        session.setQuizSet(quizSetWithOneQuestion());
        session.setPhase(QuizSessionPhase.SCOREBOARD);
        session.setCurrentQuestionIndex(0);
        session.setActiveSlot(true);
        when(quizSessionRepository.findLockedById(8L)).thenReturn(Optional.of(session));

        var response = service.nextQuestion(8L);

        assertThat(response.phase()).isEqualTo(QuizSessionPhase.FINISHED);
        assertThat(session.getActiveSlot()).isNull();
    }

    @Test
    void participantCanChangeAnswerBeforeDeadline() {
        QuizSession session = openQuestionSession(Instant.now());
        QuizQuestion question = session.getQuizSet().getQuestions().getFirst();
        QuizAnswer firstAnswer = question.getAnswers().getFirst();
        QuizAnswer secondAnswer = question.getAnswers().get(1);
        AppUser user = new AppUser();
        QuizSubmission existing = new QuizSubmission();
        existing.setSession(session);
        existing.setQuestion(question);
        existing.setUser(user);
        existing.setAnswer(firstAnswer);
        existing.setCorrect(true);

        when(quizSessionRepository.findLockedById(3L)).thenReturn(Optional.of(session));
        when(authentication.getName()).thenReturn("player@example.test");
        when(appUserRepository.findByEmail("player@example.test")).thenReturn(Optional.of(user));
        when(participantRepository.existsBySessionAndUser(session, user)).thenReturn(true);
        when(submissionRepository.findBySessionAndQuestionAndUser(session, question, user)).thenReturn(Optional.of(existing));

        service.submitAnswer(3L, secondAnswer.getId(), authentication);

        assertThat(existing.getAnswer()).isSameAs(secondAnswer);
        assertThat(existing.isCorrect()).isFalse();
        verify(submissionRepository).save(existing);
    }

    @Test
    void answerAtOrAfterDeadlineIsNotSavedAndLocksQuestion() {
        QuizSession session = openQuestionSession(Instant.now().minusSeconds(31));
        AppUser user = new AppUser();
        when(quizSessionRepository.findLockedById(4L)).thenReturn(Optional.of(session));
        when(authentication.getName()).thenReturn("late@example.test");
        when(appUserRepository.findByEmail("late@example.test")).thenReturn(Optional.of(user));
        when(participantRepository.existsBySessionAndUser(session, user)).thenReturn(true);

        var response = service.submitAnswer(4L, 10L, authentication);

        assertThat(response.phase()).isEqualTo(QuizSessionPhase.QUESTION_LOCKED);
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void schedulerLocksExpiredOpenQuestion() {
        QuizSession session = openQuestionSession(Instant.now().minusSeconds(31));
        when(quizSessionRepository.findLockedByPhase(QuizSessionPhase.QUESTION_OPEN)).thenReturn(List.of(session));

        service.lockExpiredQuestions();

        assertThat(session.getPhase()).isEqualTo(QuizSessionPhase.QUESTION_LOCKED);
    }

    @Test
    void adminCanCloseAnswersBeforeDeadline() {
        QuizSession session = openQuestionSession(Instant.now());
        when(quizSessionRepository.findLockedById(5L)).thenReturn(Optional.of(session));

        var response = service.lockQuestion(5L);

        assertThat(response.phase()).isEqualTo(QuizSessionPhase.QUESTION_LOCKED);
        assertThat(session.getPhase()).isEqualTo(QuizSessionPhase.QUESTION_LOCKED);
    }

    @Test
    void imageAboveFiveMegabytesIsRejected() {
        String oversizedImage = "data:image/png;base64," + Base64.getEncoder().encodeToString(new byte[5 * 1024 * 1024 + 1]);
        QuizSetRequest request = new QuizSetRequest("Grand visuel", List.of(new QuizQuestionRequest(
                "Question",
                oversizedImage,
                null,
                30,
                List.of(new QuizAnswerRequest("Oui", true), new QuizAnswerRequest("Non", false))
        )));

        assertThatThrownBy(() -> service.createQuizSet(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Chaque image doit peser 5 Mo maximum");
        verify(quizSetRepository, never()).save(any());
    }

    private QuizSet quizSetWithOneQuestion() {
        QuizSet quizSet = new QuizSet();
        quizSet.setTitle("Culture hippique");
        QuizQuestion question = new QuizQuestion();
        question.setQuizSet(quizSet);
        question.setText("Quel cheval a gagne ?");
        question.setDurationSeconds(30);
        QuizAnswer answer = new QuizAnswer();
        ReflectionTestUtils.setField(answer, "id", 10L);
        answer.setQuestion(question);
        answer.setText("Ourasi");
        answer.setCorrect(true);
        question.getAnswers().add(answer);
        QuizAnswer secondAnswer = new QuizAnswer();
        ReflectionTestUtils.setField(secondAnswer, "id", 11L);
        secondAnswer.setQuestion(question);
        secondAnswer.setText("Jappeloup");
        secondAnswer.setCorrect(false);
        question.getAnswers().add(secondAnswer);
        quizSet.getQuestions().add(question);
        return quizSet;
    }

    private QuizSession openQuestionSession(Instant phaseStartedAt) {
        QuizSession session = new QuizSession();
        session.setQuizSet(quizSetWithOneQuestion());
        session.setPhase(QuizSessionPhase.QUESTION_OPEN);
        session.setCurrentQuestionIndex(0);
        session.setPhaseStartedAt(phaseStartedAt);
        session.setActiveSlot(true);
        return session;
    }
}
