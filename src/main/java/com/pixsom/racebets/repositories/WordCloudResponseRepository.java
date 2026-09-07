package com.pixsom.racebets.repositories;

import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.wordcloud.WordCloudQuestion;
import com.pixsom.racebets.wordcloud.WordCloudResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WordCloudResponseRepository extends JpaRepository<WordCloudResponse, Long> {

    Optional<WordCloudResponse> findByQuestionAndUser(WordCloudQuestion question, AppUser user);

    List<WordCloudResponse> findByQuestionOrderByCreatedAtAscIdAsc(WordCloudQuestion question);

    boolean existsByQuestionAndNormalizedText(WordCloudQuestion question, String normalizedText);

    long countByQuestion(WordCloudQuestion question);

    @Modifying
    @Query("delete from WordCloudResponse response where response.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    long deleteByQuestion(WordCloudQuestion question);
}
