package com.pixsom.racebets.quiz;

import com.pixsom.racebets.admin.ConflictException;
import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.quiz.dto.QuizAnswerRequest;
import com.pixsom.racebets.quiz.dto.QuizAnswerResponse;
import com.pixsom.racebets.quiz.dto.QuizQuestionRequest;
import com.pixsom.racebets.quiz.dto.QuizQuestionResponse;
import com.pixsom.racebets.quiz.dto.QuizScoreResponse;
import com.pixsom.racebets.quiz.dto.QuizSessionSnapshotResponse;
import com.pixsom.racebets.quiz.dto.QuizSessionSummaryResponse;
import com.pixsom.racebets.quiz.dto.QuizSetDetailResponse;
import com.pixsom.racebets.quiz.dto.QuizSetListResponse;
import com.pixsom.racebets.quiz.dto.QuizSetRequest;
import com.pixsom.racebets.repositories.AppUserRepository;
import com.pixsom.racebets.repositories.QuizParticipantRepository;
import com.pixsom.racebets.repositories.QuizSessionRepository;
import com.pixsom.racebets.repositories.QuizSetRepository;
import com.pixsom.racebets.repositories.QuizSubmissionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private static final int DEFAULT_DURATION_SECONDS = 30;
    private static final int MAX_DATA_URL_LENGTH = 750_000;
    private static final Set<String> ALLOWED_IMAGE_PREFIXES = Set.of(
            "data:image/png;base64,",
            "data:image/jpeg;base64,",
            "data:image/webp;base64,"
    );
    private static final List<QuizSessionPhase> LIVE_PHASES = List.of(
            QuizSessionPhase.OPENING,
            QuizSessionPhase.QUESTION_OPEN,
            QuizSessionPhase.QUESTION_LOCKED,
            QuizSessionPhase.ANSWER_REVEALED,
            QuizSessionPhase.SCOREBOARD
    );

    private final QuizSetRepository quizSetRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizParticipantRepository participantRepository;
    private final QuizSubmissionRepository submissionRepository;
    private final AppUserRepository appUserRepository;

    public QuizService(
            QuizSetRepository quizSetRepository,
            QuizSessionRepository quizSessionRepository,
            QuizParticipantRepository participantRepository,
            QuizSubmissionRepository submissionRepository,
            AppUserRepository appUserRepository
    ) {
        this.quizSetRepository = quizSetRepository;
        this.quizSessionRepository = quizSessionRepository;
        this.participantRepository = participantRepository;
        this.submissionRepository = submissionRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<QuizSetListResponse> findQuizSets() {
        return quizSetRepository.findAll(Sort.by("createdAt").descending())
                .stream()
                .map(this::toListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizSetDetailResponse findQuizSet(Long id) {
        return toDetailResponse(findSet(id), true);
    }

    @Transactional
    public QuizSetDetailResponse createQuizSet(QuizSetRequest request) {
        QuizSet quizSet = new QuizSet();
        applySetRequest(quizSet, request);
        return toDetailResponse(quizSetRepository.save(quizSet), true);
    }

    @Transactional
    public QuizSetDetailResponse updateQuizSet(Long id, QuizSetRequest request) {
        QuizSet quizSet = findSet(id);
        quizSet.getQuestions().clear();
        applySetRequest(quizSet, request);
        return toDetailResponse(quizSetRepository.save(quizSet), true);
    }

    @Transactional
    public void deleteQuizSet(Long id) {
        QuizSet quizSet = findSet(id);
        for (QuizSession session : quizSessionRepository.findByQuizSet(quizSet)) {
            submissionRepository.deleteBySession(session);
            participantRepository.deleteBySession(session);
            quizSessionRepository.delete(session);
        }
        quizSetRepository.delete(quizSet);
    }

    @Transactional
    public QuizSessionSnapshotResponse openSession(Long quizSetId) {
        QuizSet quizSet = findSet(quizSetId);
        if (quizSet.getQuestions().isEmpty()) {
            throw new ConflictException("A quiz set must contain at least one question before launch");
        }

        QuizSession session = new QuizSession();
        session.setQuizSet(quizSet);
        session.setPhase(QuizSessionPhase.OPENING);
        session.setCurrentQuestionIndex(-1);
        session.setPhaseStartedAt(Instant.now());
        return toSnapshot(quizSessionRepository.save(session), null, true);
    }

    @Transactional
    public QuizSessionSnapshotResponse startSession(Long sessionId) {
        QuizSession session = findSession(sessionId);
        requirePhase(session, QuizSessionPhase.OPENING);
        session.setCurrentQuestionIndex(0);
        setPhase(session, QuizSessionPhase.QUESTION_OPEN);
        return toSnapshot(session, null, true);
    }

    @Transactional
    public QuizSessionSnapshotResponse lockQuestion(Long sessionId) {
        QuizSession session = findSession(sessionId);
        requirePhase(session, QuizSessionPhase.QUESTION_OPEN);
        setPhase(session, QuizSessionPhase.QUESTION_LOCKED);
        return toSnapshot(session, null, true);
    }

    @Transactional
    public QuizSessionSnapshotResponse revealAnswer(Long sessionId) {
        QuizSession session = findSession(sessionId);
        requirePhase(session, QuizSessionPhase.QUESTION_LOCKED);
        setPhase(session, QuizSessionPhase.ANSWER_REVEALED);
        return toSnapshot(session, null, true);
    }

    @Transactional
    public QuizSessionSnapshotResponse showScoreboard(Long sessionId) {
        QuizSession session = findSession(sessionId);
        requirePhase(session, QuizSessionPhase.ANSWER_REVEALED);
        setPhase(session, QuizSessionPhase.SCOREBOARD);
        return toSnapshot(session, null, true);
    }

    @Transactional
    public QuizSessionSnapshotResponse nextQuestion(Long sessionId) {
        QuizSession session = findSession(sessionId);
        requirePhase(session, QuizSessionPhase.SCOREBOARD);
        int nextIndex = session.getCurrentQuestionIndex() + 1;
        if (nextIndex >= session.getQuizSet().getQuestions().size()) {
            setPhase(session, QuizSessionPhase.FINISHED);
        } else {
            session.setCurrentQuestionIndex(nextIndex);
            setPhase(session, QuizSessionPhase.QUESTION_OPEN);
        }
        return toSnapshot(session, null, true);
    }

    @Transactional(readOnly = true)
    public List<QuizSessionSummaryResponse> findLiveSessions() {
        return quizSessionRepository.findByPhaseInOrderByCreatedAtDesc(LIVE_PHASES)
                .stream()
                .map(this::toSessionSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizSessionSnapshotResponse findSessionSnapshot(Long sessionId, Authentication authentication) {
        AppUser user = authentication == null ? null : currentUser(authentication);
        return toSnapshot(findSession(sessionId), user, false);
    }

    @Transactional
    public QuizSessionSnapshotResponse joinSession(Long sessionId, Authentication authentication) {
        QuizSession session = findSession(sessionId);
        if (session.getPhase() == QuizSessionPhase.FINISHED) {
            throw new ConflictException("This quiz session is already finished");
        }
        AppUser user = currentUser(authentication);
        if (!participantRepository.existsBySessionAndUser(session, user)) {
            QuizParticipant participant = new QuizParticipant();
            participant.setSession(session);
            participant.setUser(user);
            participantRepository.save(participant);
        }
        return toSnapshot(session, user, false);
    }

    @Transactional
    public QuizSessionSnapshotResponse submitAnswer(Long sessionId, Long answerId, Authentication authentication) {
        QuizSession session = findSession(sessionId);
        requirePhase(session, QuizSessionPhase.QUESTION_OPEN);
        AppUser user = currentUser(authentication);
        if (!participantRepository.existsBySessionAndUser(session, user)) {
            throw new ConflictException("Join the quiz session before answering");
        }

        QuizQuestion question = currentQuestion(session);
        if (submissionRepository.findBySessionAndQuestionAndUser(session, question, user).isPresent()) {
            throw new ConflictException("You already answered this question");
        }

        QuizAnswer answer = question.getAnswers().stream()
                .filter(candidate -> Objects.equals(candidate.getId(), answerId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Answer not found for current question"));

        QuizSubmission submission = new QuizSubmission();
        submission.setSession(session);
        submission.setQuestion(question);
        submission.setAnswer(answer);
        submission.setUser(user);
        submission.setCorrect(answer.isCorrect());
        submissionRepository.save(submission);

        return toSnapshot(session, user, false);
    }

    private void applySetRequest(QuizSet quizSet, QuizSetRequest request) {
        quizSet.setTitle(request.title().trim());
        quizSet.setStatus(QuizSetStatus.CREATED);

        int questionPosition = 0;
        for (QuizQuestionRequest questionRequest : request.questions()) {
            validateAnswers(questionRequest.answers());
            QuizQuestion question = new QuizQuestion();
            question.setQuizSet(quizSet);
            question.setPosition(questionPosition++);
            question.setText(questionRequest.text().trim());
            question.setQuestionImageDataUrl(normalizeImage(questionRequest.questionImageDataUrl()));
            question.setAnswerImageDataUrl(normalizeImage(questionRequest.answerImageDataUrl()));
            question.setDurationSeconds(questionRequest.durationSeconds() == null ? DEFAULT_DURATION_SECONDS : questionRequest.durationSeconds());

            int answerPosition = 0;
            for (QuizAnswerRequest answerRequest : questionRequest.answers()) {
                QuizAnswer answer = new QuizAnswer();
                answer.setQuestion(question);
                answer.setPosition(answerPosition++);
                answer.setText(answerRequest.text().trim());
                answer.setCorrect(answerRequest.correct());
                question.getAnswers().add(answer);
            }
            quizSet.getQuestions().add(question);
        }
    }

    private void validateAnswers(List<QuizAnswerRequest> answers) {
        long correctCount = answers.stream().filter(QuizAnswerRequest::correct).count();
        if (correctCount != 1) {
            throw new ConflictException("Each question must have exactly one correct answer");
        }
    }

    private String normalizeImage(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        boolean allowedType = ALLOWED_IMAGE_PREFIXES.stream().anyMatch(trimmed::startsWith);
        if (!allowedType || trimmed.length() > MAX_DATA_URL_LENGTH) {
            throw new ConflictException("Images must be png, jpeg or webp and below the configured size limit");
        }
        return trimmed;
    }

    private QuizSet findSet(Long id) {
        return quizSetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Quiz set not found"));
    }

    private QuizSession findSession(Long id) {
        return quizSessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Quiz session not found"));
    }

    private AppUser currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new NotFoundException("Authenticated user not found");
        }
        return appUserRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }

    private void requirePhase(QuizSession session, QuizSessionPhase expected) {
        if (session.getPhase() != expected) {
            throw new ConflictException("Invalid quiz session phase: expected " + expected + " but was " + session.getPhase());
        }
    }

    private void setPhase(QuizSession session, QuizSessionPhase phase) {
        session.setPhase(phase);
        session.setPhaseStartedAt(Instant.now());
    }

    private QuizQuestion currentQuestion(QuizSession session) {
        int index = session.getCurrentQuestionIndex();
        List<QuizQuestion> questions = session.getQuizSet().getQuestions();
        if (index < 0 || index >= questions.size()) {
            throw new ConflictException("No active question for this session");
        }
        return questions.get(index);
    }

    private QuizSetListResponse toListResponse(QuizSet quizSet) {
        return new QuizSetListResponse(quizSet.getId(), quizSet.getTitle(), quizSet.getStatus(), quizSet.getQuestions().size());
    }

    private QuizSetDetailResponse toDetailResponse(QuizSet quizSet, boolean includeCorrectAnswers) {
        return new QuizSetDetailResponse(
                quizSet.getId(),
                quizSet.getTitle(),
                quizSet.getStatus(),
                quizSet.getQuestions().stream().map(question -> toQuestionResponse(question, includeCorrectAnswers)).toList()
        );
    }

    private QuizQuestionResponse toQuestionResponse(QuizQuestion question, boolean includeCorrectAnswers) {
        return new QuizQuestionResponse(
                question.getId(),
                question.getPosition(),
                question.getText(),
                question.getQuestionImageDataUrl(),
                question.getAnswerImageDataUrl(),
                question.getDurationSeconds(),
                question.getAnswers().stream().map(answer -> new QuizAnswerResponse(
                        answer.getId(),
                        answer.getPosition(),
                        answer.getText(),
                        includeCorrectAnswers ? answer.isCorrect() : null
                )).toList()
        );
    }

    private QuizSessionSummaryResponse toSessionSummary(QuizSession session) {
        return new QuizSessionSummaryResponse(
                session.getId(),
                session.getQuizSet().getId(),
                session.getQuizSet().getTitle(),
                session.getPhase(),
                session.getCurrentQuestionIndex(),
                session.getQuizSet().getQuestions().size(),
                participantRepository.findBySession(session).size()
        );
    }

    private QuizSessionSnapshotResponse toSnapshot(QuizSession session, AppUser user, boolean adminView) {
        QuizQuestion question = session.getCurrentQuestionIndex() < 0 ? null : currentQuestion(session);
        boolean reveal = adminView || session.getPhase() == QuizSessionPhase.ANSWER_REVEALED
                || session.getPhase() == QuizSessionPhase.SCOREBOARD
                || session.getPhase() == QuizSessionPhase.FINISHED;
        Long selectedAnswerId = user == null || question == null ? null : submissionRepository
                .findBySessionAndQuestionAndUser(session, question, user)
                .map(submission -> submission.getAnswer().getId())
                .orElse(null);
        Long correctAnswerId = reveal && question != null ? question.getAnswers().stream()
                .filter(QuizAnswer::isCorrect)
                .map(QuizAnswer::getId)
                .findFirst()
                .orElse(null) : null;

        return new QuizSessionSnapshotResponse(
                session.getId(),
                session.getQuizSet().getId(),
                session.getQuizSet().getTitle(),
                session.getPhase(),
                session.getCurrentQuestionIndex(),
                session.getQuizSet().getQuestions().size(),
                session.getPhaseStartedAt(),
                user != null && participantRepository.existsBySessionAndUser(session, user),
                selectedAnswerId,
                correctAnswerId,
                question == null ? 0 : submissionRepository.countBySessionAndQuestion(session, question),
                question == null ? null : toQuestionResponse(question, reveal),
                scores(session)
        );
    }

    private List<QuizScoreResponse> scores(QuizSession session) {
        Map<Long, Long> scoresByUserId = submissionRepository.findBySession(session).stream()
                .filter(QuizSubmission::isCorrect)
                .collect(Collectors.groupingBy(submission -> submission.getUser().getId(), Collectors.counting()));

        return participantRepository.findBySession(session).stream()
                .map(QuizParticipant::getUser)
                .distinct()
                .map(user -> new QuizScoreResponse(
                        user.getId(),
                        (user.getName() + " " + user.getSurname()).trim(),
                        scoresByUserId.getOrDefault(user.getId(), 0L).intValue()
                ))
                .sorted(Comparator.comparingInt(QuizScoreResponse::score).reversed().thenComparing(QuizScoreResponse::displayName))
                .toList();
    }
}
