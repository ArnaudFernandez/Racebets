package com.pixsom.racebets.quiz;

import com.pixsom.racebets.entities.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "quiz_sessions")
public class QuizSession extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_set_id", nullable = false)
    private QuizSet quizSet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private QuizSessionPhase phase = QuizSessionPhase.OPENING;

    @Column(nullable = false)
    private int currentQuestionIndex = -1;

    private Instant phaseStartedAt;
}
