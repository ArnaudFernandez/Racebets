package com.pixsom.racebets.quiz;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QuizSessionScheduler {

    private final QuizService quizService;

    public QuizSessionScheduler(QuizService quizService) {
        this.quizService = quizService;
    }

    @Scheduled(fixedDelayString = "${racebets.quiz.expiry-check-delay:250}")
    public void lockExpiredQuestions() {
        quizService.lockExpiredQuestions();
    }
}
