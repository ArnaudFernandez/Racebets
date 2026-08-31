package com.pixsom.racebets.wordcloud;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "word_cloud_moderated_words",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_word_cloud_moderated_words_question_text",
                columnNames = {"question_id", "normalized_text"}
        )
)
public class WordCloudModeratedWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private WordCloudQuestion question;

    @Column(name = "normalized_text", nullable = false, length = 80)
    private String normalizedText;
}
