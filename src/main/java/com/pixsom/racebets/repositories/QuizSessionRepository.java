package com.pixsom.racebets.repositories;

import com.pixsom.racebets.quiz.QuizSession;
import com.pixsom.racebets.quiz.QuizSessionPhase;
import com.pixsom.racebets.quiz.QuizSet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {
    List<QuizSession> findByPhaseInOrderByCreatedAtDesc(Collection<QuizSessionPhase> phases);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from QuizSession session where session.id = :id")
    Optional<QuizSession> findLockedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select session from QuizSession session where session.id = :id")
    Optional<QuizSession> findReadLockedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from QuizSession session where session.phase = :phase")
    List<QuizSession> findLockedByPhase(@Param("phase") QuizSessionPhase phase);

    boolean existsByPhaseIn(Collection<QuizSessionPhase> phases);

    boolean existsByQuizSetAndPhaseIn(QuizSet quizSet, Collection<QuizSessionPhase> phases);

    boolean existsByQuizSet(QuizSet quizSet);

    List<QuizSession> findByQuizSet(QuizSet quizSet);
}
