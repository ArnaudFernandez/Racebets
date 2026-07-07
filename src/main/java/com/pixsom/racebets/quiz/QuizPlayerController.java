package com.pixsom.racebets.quiz;

import com.pixsom.racebets.quiz.dto.QuizAnswerSubmitRequest;
import com.pixsom.racebets.quiz.dto.QuizSessionSnapshotResponse;
import com.pixsom.racebets.quiz.dto.QuizSessionSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
public class QuizPlayerController {

    private final QuizService quizService;

    public QuizPlayerController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/live")
    public List<QuizSessionSummaryResponse> liveSessions() {
        return quizService.findLiveSessions();
    }

    @GetMapping("/sessions/{sessionId}")
    public QuizSessionSnapshotResponse snapshot(@PathVariable Long sessionId, Authentication authentication) {
        return quizService.findSessionSnapshot(sessionId, authentication);
    }

    @PostMapping("/sessions/{sessionId}/join")
    public QuizSessionSnapshotResponse join(@PathVariable Long sessionId, Authentication authentication) {
        return quizService.joinSession(sessionId, authentication);
    }

    @PostMapping("/sessions/{sessionId}/answers")
    public QuizSessionSnapshotResponse answer(
            @PathVariable Long sessionId,
            @Valid @RequestBody QuizAnswerSubmitRequest request,
            Authentication authentication
    ) {
        return quizService.submitAnswer(sessionId, request.answerId(), authentication);
    }
}
