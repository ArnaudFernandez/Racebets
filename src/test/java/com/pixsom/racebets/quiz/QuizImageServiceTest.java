package com.pixsom.racebets.quiz;

import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.repositories.QuizQuestionRepository;
import com.pixsom.racebets.repositories.QuizSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizImageServiceTest {

    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
    };

    @Mock QuizSessionRepository sessionRepository;
    @Mock QuizQuestionRepository questionRepository;

    private QuizImageService service;
    private QuizSession session;
    private QuizQuestion question;

    @BeforeEach
    void setUp() {
        service = new QuizImageService(sessionRepository, questionRepository);

        QuizSet quizSet = new QuizSet();
        ReflectionTestUtils.setField(quizSet, "id", 10L);

        session = new QuizSession();
        session.setQuizSet(quizSet);
        session.setCurrentQuestionIndex(0);

        question = new QuizQuestion();
        ReflectionTestUtils.setField(question, "id", 20L);
        question.setQuestionImageDataUrl(dataUrl(PNG_BYTES));
        question.setAnswerImageDataUrl(dataUrl(PNG_BYTES));

        when(sessionRepository.findById(30L)).thenReturn(Optional.of(session));
    }

    @Test
    void returnsTheCurrentQuestionImage() {
        session.setPhase(QuizSessionPhase.QUESTION_OPEN);
        when(questionRepository.findQuestionImageDataUrl(10L, 0, 20L))
                .thenReturn(Optional.of(question.getQuestionImageDataUrl()));

        QuizImageService.ImageResponse response = service.findSessionImage(
                30L, 20L, QuizImageService.ImageKind.QUESTION);

        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.bytes()).isEqualTo(PNG_BYTES);
        assertThat(response.etag()).startsWith("\"").endsWith("\"");
    }

    @Test
    void hidesTheAnswerImageUntilTheAnswerIsRevealed() {
        session.setPhase(QuizSessionPhase.QUESTION_LOCKED);

        assertThatThrownBy(() -> service.findSessionImage(30L, 20L, QuizImageService.ImageKind.ANSWER))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void returnsTheAnswerImageAfterReveal() {
        session.setPhase(QuizSessionPhase.ANSWER_REVEALED);
        when(questionRepository.findAnswerImageDataUrl(10L, 0, 20L))
                .thenReturn(Optional.of(question.getAnswerImageDataUrl()));

        QuizImageService.ImageResponse response = service.findSessionImage(
                30L, 20L, QuizImageService.ImageKind.ANSWER);

        assertThat(response.bytes()).isEqualTo(PNG_BYTES);
    }

    @Test
    void rejectsAnImageWhoseDeclaredTypeDoesNotMatchItsSignature() {
        session.setPhase(QuizSessionPhase.QUESTION_OPEN);
        question.setQuestionImageDataUrl(dataUrl(new byte[]{0x01, 0x02, 0x03}));
        when(questionRepository.findQuestionImageDataUrl(10L, 0, 20L))
                .thenReturn(Optional.of(question.getQuestionImageDataUrl()));

        assertThatThrownBy(() -> service.findSessionImage(30L, 20L, QuizImageService.ImageKind.QUESTION))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsMalformedDataUrls() {
        session.setPhase(QuizSessionPhase.QUESTION_OPEN);
        question.setQuestionImageDataUrl("x;base64,AAAA");
        when(questionRepository.findQuestionImageDataUrl(10L, 0, 20L))
                .thenReturn(Optional.of(question.getQuestionImageDataUrl()));

        assertThatThrownBy(() -> service.findSessionImage(30L, 20L, QuizImageService.ImageKind.QUESTION))
                .isInstanceOf(NotFoundException.class);
    }

    private String dataUrl(byte[] bytes) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
