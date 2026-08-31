package com.pixsom.racebets.repositories;

import com.pixsom.racebets.wordcloud.WordCloudModeratedWord;
import com.pixsom.racebets.wordcloud.WordCloudQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WordCloudModeratedWordRepository extends JpaRepository<WordCloudModeratedWord, Long> {

    List<WordCloudModeratedWord> findByQuestion(WordCloudQuestion question);

    boolean existsByQuestionAndNormalizedText(WordCloudQuestion question, String normalizedText);

    long deleteByQuestion(WordCloudQuestion question);
}
