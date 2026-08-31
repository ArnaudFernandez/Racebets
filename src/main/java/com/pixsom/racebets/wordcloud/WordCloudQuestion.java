package com.pixsom.racebets.wordcloud;

import com.pixsom.racebets.entities.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "word_cloud_questions")
public class WordCloudQuestion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false, length = 300)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WordCloudQuestionStatus status = WordCloudQuestionStatus.DRAFT;

    @Column(name = "active_slot", unique = true)
    private Boolean activeSlot;

    private Instant openedAt;

    private Instant revealedAt;

    private Instant closedAt;
}
