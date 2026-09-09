package com.pixsom.racebets.repositories;

import com.pixsom.racebets.quiz.QuizSet;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizSetRepository extends JpaRepository<QuizSet, Long> {

    List<QuizSet> findByArchivedFalse(Sort sort);
}
