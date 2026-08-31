package com.pixsom.racebets.repositories;

import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.wordcloud.WordCloudQuestion;
import com.pixsom.racebets.wordcloud.WordCloudResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WordCloudResponseRepository extends JpaRepository<WordCloudResponse, Long> {

    Optional<WordCloudResponse> findByQuestionAndUser(WordCloudQuestion question, AppUser user);

    List<WordCloudResponse> findByQuestionOrderByCreatedAtAscIdAsc(WordCloudQuestion question);

    boolean existsByQuestionAndNormalizedText(WordCloudQuestion question, String normalizedText);

    long countByQuestion(WordCloudQuestion question);

    long deleteByQuestion(WordCloudQuestion question);
}
