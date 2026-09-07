package com.pixsom.racebets.quiz;

import com.pixsom.racebets.quiz.dto.QuizAnswerSubmitRequest;
import com.pixsom.racebets.quiz.dto.QuizAnswerSubmissionResponse;
import com.pixsom.racebets.quiz.dto.QuizSessionSnapshotResponse;
import com.pixsom.racebets.quiz.dto.QuizSessionSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
        return quizService.findPlayerLiveSessions();
    }

    @GetMapping("/sessions/{sessionId}")
    public QuizSessionSnapshotResponse snapshot(@PathVariable Long sessionId, @AuthenticationPrincipal Jwt jwt) {
        return quizService.findSessionSnapshot(sessionId, userId(jwt));
    }

    @PostMapping("/sessions/{sessionId}/join")
    public QuizSessionSnapshotResponse join(@PathVariable Long sessionId, @AuthenticationPrincipal Jwt jwt) {
        Long userId = userId(jwt);
        quizService.joinSession(sessionId, userId);
        return quizService.findSessionSnapshot(sessionId, userId);
    }

    @PostMapping("/sessions/{sessionId}/answers")
    public QuizAnswerSubmissionResponse answer(
            @PathVariable Long sessionId,
            @Valid @RequestBody QuizAnswerSubmitRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return quizService.submitAnswer(sessionId, request.answerId(), userId(jwt));
    }

    private Long userId(Jwt jwt) {
        return jwt.getClaim("userId");
    }
}
