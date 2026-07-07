package com.pixsom.racebets.repositories;

import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.quiz.QuizQuestion;
import com.pixsom.racebets.quiz.QuizSession;
import com.pixsom.racebets.quiz.QuizSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {
    Optional<QuizSubmission> findBySessionAndQuestionAndUser(QuizSession session, QuizQuestion question, AppUser user);

    List<QuizSubmission> findBySession(QuizSession session);

    long countBySessionAndQuestion(QuizSession session, QuizQuestion question);

    void deleteBySession(QuizSession session);
}
