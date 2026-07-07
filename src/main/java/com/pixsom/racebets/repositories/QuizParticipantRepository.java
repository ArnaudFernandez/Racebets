package com.pixsom.racebets.repositories;

import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.quiz.QuizParticipant;
import com.pixsom.racebets.quiz.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizParticipantRepository extends JpaRepository<QuizParticipant, Long> {
    boolean existsBySessionAndUser(QuizSession session, AppUser user);

    Optional<QuizParticipant> findBySessionAndUser(QuizSession session, AppUser user);

    List<QuizParticipant> findBySession(QuizSession session);

    void deleteBySession(QuizSession session);
}
