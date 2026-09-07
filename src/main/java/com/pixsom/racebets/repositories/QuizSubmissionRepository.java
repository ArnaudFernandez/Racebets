package com.pixsom.racebets.repositories;

import com.pixsom.racebets.quiz.QuizSession;
import com.pixsom.racebets.quiz.QuizSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {

    interface QuizScoreView {
        Long getUserId();

        String getDisplayName();

        long getScore();
    }

    Optional<QuizSubmission> findBySession_IdAndQuestion_IdAndUser_Id(Long sessionId, Long questionId, Long userId);

    long countBySession_IdAndQuestion_Id(Long sessionId, Long questionId);

    @Query(value = """
            SELECT participant.user_id AS "userId",
                   TRIM(CONCAT(app_user.name, ' ', app_user.surname)) AS "displayName",
                   COUNT(CASE WHEN submission.correct THEN 1 END) AS "score"
            FROM quiz_participants participant
            JOIN app_user ON app_user.id = participant.user_id
            LEFT JOIN quiz_submissions submission
                   ON submission.session_id = participant.session_id
                  AND submission.user_id = participant.user_id
            WHERE participant.session_id = :sessionId
            GROUP BY participant.user_id, app_user.name, app_user.surname
            ORDER BY COUNT(CASE WHEN submission.correct THEN 1 END) DESC,
                     TRIM(CONCAT(app_user.name, ' ', app_user.surname)) ASC
            """, nativeQuery = true)
    List<QuizScoreView> findScoresBySessionId(@Param("sessionId") Long sessionId);

    @Modifying
    @Query("delete from QuizSubmission submission where submission.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    void deleteBySession(QuizSession session);
}
