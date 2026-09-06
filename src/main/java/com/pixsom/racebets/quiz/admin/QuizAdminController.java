package com.pixsom.racebets.quiz.admin;

import com.pixsom.racebets.quiz.QuizService;
import com.pixsom.racebets.quiz.dto.QuizSetDetailResponse;
import com.pixsom.racebets.quiz.dto.QuizSetListResponse;
import com.pixsom.racebets.quiz.dto.QuizSetRequest;
import com.pixsom.racebets.quiz.dto.QuizSessionSnapshotResponse;
import com.pixsom.racebets.quiz.dto.QuizSessionSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/quizzes")
public class QuizAdminController {

    private final QuizService quizService;

    public QuizAdminController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping
    public List<QuizSetListResponse> findAll() {
        return quizService.findQuizSets();
    }

    @GetMapping("/{id}")
    public QuizSetDetailResponse findById(@PathVariable Long id) {
        return quizService.findQuizSet(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuizSetDetailResponse create(@Valid @RequestBody QuizSetRequest request) {
        return quizService.createQuizSet(request);
    }

    @PutMapping("/{id}")
    public QuizSetDetailResponse update(@PathVariable Long id, @Valid @RequestBody QuizSetRequest request) {
        return quizService.updateQuizSet(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        quizService.deleteQuizSet(id);
    }

    @PostMapping("/{id}/open")
    @ResponseStatus(HttpStatus.CREATED)
    public QuizSessionSnapshotResponse openSession(@PathVariable Long id) {
        return quizService.openSession(id);
    }

    @GetMapping("/sessions/live")
    public List<QuizSessionSummaryResponse> liveSessions() {
        return quizService.findLiveSessions();
    }

    @GetMapping("/sessions/{sessionId}")
    public QuizSessionSnapshotResponse session(@PathVariable Long sessionId) {
        return quizService.findAdminSessionSnapshot(sessionId);
    }

    @PostMapping("/sessions/{sessionId}/start")
    public QuizSessionSnapshotResponse start(@PathVariable Long sessionId) {
        return quizService.startSession(sessionId);
    }

    @PostMapping("/sessions/{sessionId}/lock")
    public QuizSessionSnapshotResponse lock(@PathVariable Long sessionId) {
        return quizService.lockQuestion(sessionId);
    }

    @PostMapping("/sessions/{sessionId}/reveal")
    public QuizSessionSnapshotResponse reveal(@PathVariable Long sessionId) {
        return quizService.revealAnswer(sessionId);
    }

    @PostMapping("/sessions/{sessionId}/scoreboard")
    public QuizSessionSnapshotResponse scoreboard(@PathVariable Long sessionId) {
        return quizService.showScoreboard(sessionId);
    }

    @PostMapping("/sessions/{sessionId}/next")
    public QuizSessionSnapshotResponse next(@PathVariable Long sessionId) {
        return quizService.nextQuestion(sessionId);
    }

    @PostMapping("/sessions/{sessionId}/stop")
    public QuizSessionSnapshotResponse stop(@PathVariable Long sessionId) {
        return quizService.stopSession(sessionId);
    }
}
