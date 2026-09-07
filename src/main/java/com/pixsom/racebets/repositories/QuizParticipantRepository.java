package com.pixsom.racebets.repositories;

import com.pixsom.racebets.quiz.QuizParticipant;
import com.pixsom.racebets.quiz.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface QuizParticipantRepository extends JpaRepository<QuizParticipant, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select participant from QuizParticipant participant where participant.session.id = :sessionId and participant.user.id = :userId")
    Optional<QuizParticipant> findLockedBySessionIdAndUserId(@Param("sessionId") Long sessionId,
                                                              @Param("userId") Long userId);

    boolean existsBySession_IdAndUser_Id(Long sessionId, Long userId);

    long countBySession(QuizSession session);

    @Modifying
    @Query("delete from QuizParticipant participant where participant.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    void deleteBySession(QuizSession session);
}
