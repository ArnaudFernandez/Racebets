package com.pixsom.racebets.repositories;

import com.pixsom.racebets.wordcloud.WordCloudQuestion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WordCloudQuestionRepository extends JpaRepository<WordCloudQuestion, Long> {

    Optional<WordCloudQuestion> findByActiveSlotTrue();

    boolean existsByActiveSlotTrue();

    Optional<WordCloudQuestion> findFirstByText(String text);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select question from WordCloudQuestion question where question.id = :id")
    Optional<WordCloudQuestion> findLockedById(@Param("id") Long id);
}
