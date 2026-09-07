package com.pixsom.racebets.repositories;

import com.pixsom.racebets.quiz.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    List<QuizAnswer> findByQuestion_IdOrderByPositionAsc(Long questionId);
}
