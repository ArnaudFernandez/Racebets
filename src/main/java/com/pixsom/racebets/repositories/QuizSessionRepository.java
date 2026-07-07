package com.pixsom.racebets.repositories;

import com.pixsom.racebets.quiz.QuizSession;
import com.pixsom.racebets.quiz.QuizSessionPhase;
import com.pixsom.racebets.quiz.QuizSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {
    List<QuizSession> findByPhaseInOrderByCreatedAtDesc(Collection<QuizSessionPhase> phases);

    List<QuizSession> findByQuizSet(QuizSet quizSet);
}
