package com.pixsom.racebets.repositories;

import com.pixsom.racebets.quiz.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    interface QuizLiveQuestionView {
        Long getId();

        int getPosition();

        String getText();

        int getDurationSeconds();

        boolean getQuestionImagePresent();

        boolean getAnswerImagePresent();
    }

    @Query("""
            SELECT question.id AS id,
                   question.position AS position,
                   question.text AS text,
                   question.durationSeconds AS durationSeconds,
                   CASE WHEN question.questionImageDataUrl IS NULL THEN false ELSE true END AS questionImagePresent,
                   CASE WHEN question.answerImageDataUrl IS NULL THEN false ELSE true END AS answerImagePresent
            FROM QuizQuestion question
            WHERE question.quizSet.id = :quizSetId AND question.position = :position
            """)
    Optional<QuizLiveQuestionView> findLiveByQuizSetIdAndPosition(@Param("quizSetId") Long quizSetId,
                                                                  @Param("position") int position);

    @Query("""
            SELECT question.questionImageDataUrl
            FROM QuizQuestion question
            WHERE question.quizSet.id = :quizSetId
              AND question.position = :position
              AND question.id = :questionId
            """)
    Optional<String> findQuestionImageDataUrl(@Param("quizSetId") Long quizSetId,
                                               @Param("position") int position,
                                               @Param("questionId") Long questionId);

    @Query("""
            SELECT question.answerImageDataUrl
            FROM QuizQuestion question
            WHERE question.quizSet.id = :quizSetId
              AND question.position = :position
              AND question.id = :questionId
            """)
    Optional<String> findAnswerImageDataUrl(@Param("quizSetId") Long quizSetId,
                                             @Param("position") int position,
                                             @Param("questionId") Long questionId);

    long countByQuizSet_Id(Long quizSetId);
}
