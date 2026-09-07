package com.pixsom.racebets.quiz;

import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.enums.Role;
import com.pixsom.racebets.repositories.AppUserRepository;
import com.pixsom.racebets.repositories.QuizParticipantRepository;
import com.pixsom.racebets.repositories.QuizQuestionRepository;
import com.pixsom.racebets.repositories.QuizSessionRepository;
import com.pixsom.racebets.repositories.QuizSetRepository;
import com.pixsom.racebets.repositories.QuizSubmissionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class QuizRepositoryTest {

    @Autowired AppUserRepository userRepository;
    @Autowired QuizSetRepository quizSetRepository;
    @Autowired QuizSessionRepository sessionRepository;
    @Autowired QuizParticipantRepository participantRepository;
    @Autowired QuizQuestionRepository questionRepository;
    @Autowired QuizSubmissionRepository submissionRepository;
    @Autowired EntityManager entityManager;

    @Test
    void liveQueriesReturnMetadataImagesAndScoresWithoutHydratingSnapshotsWithLobs() {
        AppUser winner = userRepository.save(user("Ada", "Lovelace", "ada@example.test"));
        AppUser waiting = userRepository.save(user("Grace", "Hopper", "grace@example.test"));

        QuizSet quizSet = new QuizSet();
        quizSet.setTitle("Performance");
        QuizQuestion question = new QuizQuestion();
        question.setQuizSet(quizSet);
        question.setPosition(0);
        question.setText("Question legere");
        question.setDurationSeconds(30);
        question.setQuestionImageDataUrl("data:image/png;base64,question-data");
        question.setAnswerImageDataUrl("data:image/png;base64,answer-data");
        QuizAnswer answer = new QuizAnswer();
        answer.setQuestion(question);
        answer.setPosition(0);
        answer.setText("Oui");
        answer.setCorrect(true);
        question.getAnswers().add(answer);
        quizSet.getQuestions().add(question);
        quizSetRepository.save(quizSet);

        QuizSession session = new QuizSession();
        session.setQuizSet(quizSet);
        session.setPhase(QuizSessionPhase.SCOREBOARD);
        session.setCurrentQuestionIndex(0);
        sessionRepository.save(session);

        participantRepository.save(participant(session, winner));
        participantRepository.save(participant(session, waiting));

        QuizSubmission submission = new QuizSubmission();
        submission.setSession(session);
        submission.setQuestion(question);
        submission.setAnswer(answer);
        submission.setUser(winner);
        submission.setCorrect(true);
        submissionRepository.save(submission);

        entityManager.flush();
        entityManager.clear();

        QuizQuestionRepository.QuizLiveQuestionView liveQuestion = questionRepository
                .findLiveByQuizSetIdAndPosition(quizSet.getId(), 0)
                .orElseThrow();
        assertThat(liveQuestion.getText()).isEqualTo("Question legere");
        assertThat(liveQuestion.getQuestionImagePresent()).isTrue();
        assertThat(liveQuestion.getAnswerImagePresent()).isTrue();
        assertThat(questionRepository.findQuestionImageDataUrl(quizSet.getId(), 0, question.getId()))
                .contains("data:image/png;base64,question-data");
        assertThat(questionRepository.findAnswerImageDataUrl(quizSet.getId(), 0, question.getId()))
                .contains("data:image/png;base64,answer-data");

        List<QuizSubmissionRepository.QuizScoreView> scores = submissionRepository
                .findScoresBySessionId(session.getId());
        assertThat(scores).extracting(QuizSubmissionRepository.QuizScoreView::getDisplayName)
                .containsExactly("Ada Lovelace", "Grace Hopper");
        assertThat(scores).extracting(QuizSubmissionRepository.QuizScoreView::getScore)
                .containsExactly(1L, 0L);
    }

    private AppUser user(String name, String surname, String email) {
        AppUser user = new AppUser();
        user.setName(name);
        user.setSurname(surname);
        user.setEmail(email);
        user.setPresent(true);
        user.setRoles(Set.of(Role.USER));
        return user;
    }

    private QuizParticipant participant(QuizSession session, AppUser user) {
        QuizParticipant participant = new QuizParticipant();
        participant.setSession(session);
        participant.setUser(user);
        return participant;
    }
}
