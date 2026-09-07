package com.pixsom.racebets.quiz;

import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.repositories.QuizQuestionRepository;
import com.pixsom.racebets.repositories.QuizSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

@Service
public class QuizImageService {

    private static final Set<QuizSessionPhase> ANSWER_IMAGE_PHASES = Set.of(
            QuizSessionPhase.ANSWER_REVEALED,
            QuizSessionPhase.SCOREBOARD,
            QuizSessionPhase.FINISHED
    );

    private final QuizSessionRepository sessionRepository;
    private final QuizQuestionRepository questionRepository;

    public QuizImageService(QuizSessionRepository sessionRepository, QuizQuestionRepository questionRepository) {
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public ImageResponse findSessionImage(Long sessionId, Long questionId, ImageKind kind) {
        QuizSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Image not found"));
        if (session.getCurrentQuestionIndex() < 0) {
            throw new NotFoundException("Image not found");
        }

        if (kind == ImageKind.ANSWER && !ANSWER_IMAGE_PHASES.contains(session.getPhase())) {
            throw new NotFoundException("Image not found");
        }

        Long quizSetId = session.getQuizSet().getId();
        int position = session.getCurrentQuestionIndex();
        String dataUrl = (kind == ImageKind.QUESTION
                ? questionRepository.findQuestionImageDataUrl(quizSetId, position, questionId)
                : questionRepository.findAnswerImageDataUrl(quizSetId, position, questionId))
                .orElseThrow(() -> new NotFoundException("Image not found"));
        try {
            QuizImageData image = QuizImageData.decode(dataUrl);
            return new ImageResponse(image.contentType(), image.bytes(), etag(image.bytes()));
        } catch (IllegalArgumentException exception) {
            throw new NotFoundException("Image not found");
        }
    }

    private String etag(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return '"' + HexFormat.of().formatHex(digest) + '"';
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public enum ImageKind {
        QUESTION,
        ANSWER
    }

    public record ImageResponse(String contentType, byte[] bytes, String etag) {
    }
}
