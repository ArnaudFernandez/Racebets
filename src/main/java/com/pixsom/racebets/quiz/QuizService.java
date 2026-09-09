package com.pixsom.racebets.quiz;

import com.pixsom.racebets.admin.ConflictException;
import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.app.AppFeatureSettingsService;
import com.pixsom.racebets.app.AppMode;
import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.quiz.dto.QuizAnswerRequest;
import com.pixsom.racebets.quiz.dto.QuizAnswerResponse;
import com.pixsom.racebets.quiz.dto.QuizAnswerSubmissionResponse;
import com.pixsom.racebets.quiz.dto.QuizLiveQuestionResponse;
import com.pixsom.racebets.quiz.dto.QuizQuestionRequest;
import com.pixsom.racebets.quiz.dto.QuizQuestionResponse;
import com.pixsom.racebets.quiz.dto.QuizScoreResponse;
import com.pixsom.racebets.quiz.dto.QuizSessionSnapshotResponse;
import com.pixsom.racebets.quiz.dto.QuizSessionSummaryResponse;
import com.pixsom.racebets.quiz.dto.QuizSetDetailResponse;
import com.pixsom.racebets.quiz.dto.QuizSetListResponse;
import com.pixsom.racebets.quiz.dto.QuizSetRequest;
import com.pixsom.racebets.repositories.AppUserRepository;
import com.pixsom.racebets.repositories.QuizAnswerRepository;
import com.pixsom.racebets.repositories.QuizParticipantRepository;
import com.pixsom.racebets.repositories.QuizQuestionRepository;
import com.pixsom.racebets.repositories.QuizSessionRepository;
import com.pixsom.racebets.repositories.QuizSetRepository;
import com.pixsom.racebets.repositories.QuizSubmissionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class QuizService {

    private static final int DEFAULT_DURATION_SECONDS = 30;
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final List<QuizSessionPhase> LIVE_PHASES = List.of(
            QuizSessionPhase.OPENING,
            QuizSessionPhase.QUESTION_OPEN,
            QuizSessionPhase.QUESTION_LOCKED,
            QuizSessionPhase.ANSWER_REVEALED,
            QuizSessionPhase.SCOREBOARD
    );

    private final QuizSetRepository quizSetRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizAnswerRepository answerRepository;
    private final QuizParticipantRepository participantRepository;
    private final QuizSubmissionRepository submissionRepository;
    private final AppUserRepository appUserRepository;
    private final AppFeatureSettingsService featureSettingsService;

    public QuizService(
            QuizSetRepository quizSetRepository,
            QuizSessionRepository quizSessionRepository,
            QuizQuestionRepository questionRepository,
            QuizAnswerRepository answerRepository,
            QuizParticipantRepository participantRepository,
            QuizSubmissionRepository submissionRepository,
            AppUserRepository appUserRepository,
            AppFeatureSettingsService featureSettingsService
    ) {
        this.quizSetRepository = quizSetRepository;
        this.quizSessionRepository = quizSessionRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.participantRepository = participantRepository;
        this.submissionRepository = submissionRepository;
        this.appUserRepository = appUserRepository;
        this.featureSettingsService = featureSettingsService;
    }

    @Transactional(readOnly = true)
    public List<QuizSetListResponse> findQuizSets() {
        return quizSetRepository.findByArchivedFalse(Sort.by("createdAt").descending())
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
        requireNoLiveSession(quizSet, "Un questionnaire utilise par la session live ne peut pas etre modifie");

        if (quizSessionRepository.existsByQuizSet(quizSet)) {
            quizSet.setArchived(true);
            QuizSet replacement = new QuizSet();
            applySetRequest(replacement, request);
            return toDetailResponse(quizSetRepository.save(replacement), true);
        }

        quizSet.getQuestions().clear();
        applySetRequest(quizSet, request);
        return toDetailResponse(quizSetRepository.save(quizSet), true);
    }

    @Transactional
    public void deleteQuizSet(Long id) {
        QuizSet quizSet = findSet(id);
        requireNoLiveSession(quizSet, "Le questionnaire de la session live ne peut pas etre supprime");
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
        if (quizSet.isArchived()) {
            throw new ConflictException("Cette version du questionnaire est archivee");
        }
        if (quizSet.getQuestions().isEmpty()) {
            throw new ConflictException("A quiz set must contain at least one question before launch");
        }
        if (quizSessionRepository.existsByPhaseIn(LIVE_PHASES)) {
            throw new ConflictException("Une session live est deja en cours. Terminez-la avant d'en lancer une autre.");
        }

        QuizSession session = new QuizSession();
        session.setQuizSet(quizSet);
        session.setPhase(QuizSessionPhase.OPENING);
        session.setCurrentQuestionIndex(-1);
        session.setPhaseStartedAt(Instant.now());
        session.setActiveSlot(true);
        try {
            return toSnapshot(quizSessionRepository.saveAndFlush(session), null, true);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Une session live est deja en cours. Terminez-la avant d'en lancer une autre.");
        }
    }

    @Transactional
    public QuizSessionSnapshotResponse startSession(Long sessionId) {
        QuizSession session = findSessionForUpdate(sessionId);
        requirePhase(session, QuizSessionPhase.OPENING);
        session.setCurrentQuestionIndex(0);
        setPhase(session, QuizSessionPhase.QUESTION_OPEN);
        return toSnapshot(session, null, true);
    }

    @Transactional
    public QuizSessionSnapshotResponse lockQuestion(Long sessionId) {
        QuizSession session = findSessionForUpdate(sessionId);
        requirePhase(session, QuizSessionPhase.QUESTION_OPEN);
        setPhase(session, QuizSessionPhase.QUESTION_LOCKED);
        return toSnapshot(session, null, true);
    }

    @Transactional
    public QuizSessionSnapshotResponse revealAnswer(Long sessionId) {
        QuizSession session = findSessionForUpdate(sessionId);
        requirePhase(session, QuizSessionPhase.QUESTION_LOCKED);
        setPhase(session, QuizSessionPhase.ANSWER_REVEALED);
        return toSnapshot(session, null, true);
    }

    @Transactional
    public QuizSessionSnapshotResponse showScoreboard(Long sessionId) {
        QuizSession session = findSessionForUpdate(sessionId);
        requirePhase(session, QuizSessionPhase.ANSWER_REVEALED);
        setPhase(session, QuizSessionPhase.SCOREBOARD);
        return toSnapshot(session, null, true);
    }

    @Transactional
    public QuizSessionSnapshotResponse nextQuestion(Long sessionId) {
        QuizSession session = findSessionForUpdate(sessionId);
        requirePhase(session, QuizSessionPhase.SCOREBOARD);
        int nextIndex = session.getCurrentQuestionIndex() + 1;
        if (nextIndex >= questionCount(session)) {
            setPhase(session, QuizSessionPhase.FINISHED);
        } else {
            session.setCurrentQuestionIndex(nextIndex);
            setPhase(session, QuizSessionPhase.QUESTION_OPEN);
        }
        return toSnapshot(session, null, true);
    }

    @Transactional
    public QuizSessionSnapshotResponse stopSession(Long sessionId) {
        QuizSession session = findSessionForUpdate(sessionId);
        if (!LIVE_PHASES.contains(session.getPhase())) {
            throw new ConflictException("Seule une session de quiz active peut etre arretee");
        }
        setPhase(session, QuizSessionPhase.CANCELLED);
        return toSnapshot(session, null, true);
    }

    @Transactional(readOnly = true)
    public List<QuizSessionSummaryResponse> findLiveSessions() {
        return quizSessionRepository.findByPhaseInOrderByCreatedAtDesc(LIVE_PHASES)
                .stream()
                .map(this::toSessionSummary)
                .toList();
    }

    @Transactional
    public List<QuizSessionSummaryResponse> findPlayerLiveSessions() {
        featureSettingsService.requireActiveMode(AppMode.QUIZ);
        return findLiveSessions();
    }

    @Transactional(readOnly = true)
    public QuizSessionSnapshotResponse findAdminSessionSnapshot(Long sessionId) {
        return toSnapshot(findSession(sessionId), null, true);
    }

    @Transactional
    public QuizSessionSnapshotResponse findSessionSnapshot(Long sessionId, Long userId) {
        featureSettingsService.requireActiveMode(AppMode.QUIZ);
        return toSnapshot(findSession(sessionId), userId, false);
    }

    @Transactional
    public void joinSession(Long sessionId, Long userId) {
        featureSettingsService.requireActiveMode(AppMode.QUIZ);
        QuizSession session = findSessionForRead(sessionId);
        if (!LIVE_PHASES.contains(session.getPhase())) {
            throw new ConflictException("This quiz session is no longer active");
        }
        AppUser user = currentUserForUpdate(userId);
        if (!participantRepository.existsBySession_IdAndUser_Id(sessionId, userId)) {
            QuizParticipant participant = new QuizParticipant();
            participant.setSession(session);
            participant.setUser(user);
            participantRepository.save(participant);
        }
    }

    @Transactional
    public QuizAnswerSubmissionResponse submitAnswer(Long sessionId, Long answerId, Long userId) {
        featureSettingsService.requireActiveMode(AppMode.QUIZ);
        QuizSession session = findSessionForRead(sessionId);
        requirePhase(session, QuizSessionPhase.QUESTION_OPEN);
        QuizParticipant participant = participantRepository.findLockedBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ConflictException("Join the quiz session before answering"));

        LiveQuestion question = currentQuestion(session);
        Instant now = Instant.now();
        if (isQuestionExpired(session, question, now)) {
            return new QuizAnswerSubmissionResponse(null, session.getPhase(), now);
        }

        QuizAnswer answer = question.answers().stream()
                .filter(candidate -> Objects.equals(candidate.getId(), answerId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Answer not found for current question"));

        QuizSubmission submission = submissionRepository.findBySession_IdAndQuestion_IdAndUser_Id(
                        sessionId, question.id(), userId)
                .orElseGet(() -> {
                    QuizSubmission created = new QuizSubmission();
                    created.setSession(session);
                    created.setQuestion(question.entity() == null
                            ? questionRepository.getReferenceById(question.id())
                            : question.entity());
                    created.setUser(participant.getUser());
                    return created;
                });
        submission.setAnswer(answer);
        submission.setCorrect(answer.isCorrect());
        submissionRepository.save(submission);

        return new QuizAnswerSubmissionResponse(answerId, session.getPhase(), now);
    }

    @Transactional
    public void lockExpiredQuestions() {
        Instant now = Instant.now();
        quizSessionRepository.findLockedByPhase(QuizSessionPhase.QUESTION_OPEN).stream()
                .filter(session -> isQuestionExpired(session, currentQuestion(session), now))
                .forEach(session -> setPhase(session, QuizSessionPhase.QUESTION_LOCKED));
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
        try {
            QuizImageData.decode(trimmed, MAX_IMAGE_BYTES);
        } catch (IllegalArgumentException exception) {
            throw new ConflictException(exception.getMessage());
        }
        return trimmed;
    }

    private void requireNoLiveSession(QuizSet quizSet, String message) {
        if (quizSessionRepository.existsByQuizSetAndPhaseIn(quizSet, LIVE_PHASES)) {
            throw new ConflictException(message);
        }
    }

    private QuizSet findSet(Long id) {
        return quizSetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Quiz set not found"));
    }

    private QuizSession findSession(Long id) {
        return quizSessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Quiz session not found"));
    }

    private QuizSession findSessionForUpdate(Long id) {
        return quizSessionRepository.findLockedById(id)
                .orElseThrow(() -> new NotFoundException("Quiz session not found"));
    }

    private QuizSession findSessionForRead(Long id) {
        return quizSessionRepository.findReadLockedById(id)
                .orElseThrow(() -> new NotFoundException("Quiz session not found"));
    }

    private AppUser currentUserForUpdate(Long userId) {
        if (userId == null) {
            throw new NotFoundException("Authenticated user not found");
        }
        return appUserRepository.findLockedById(userId)
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
        boolean terminal = phase == QuizSessionPhase.FINISHED || phase == QuizSessionPhase.CANCELLED;
        session.setActiveSlot(terminal ? null : true);
    }

    private boolean isQuestionExpired(QuizSession session, LiveQuestion question, Instant now) {
        if (session.getPhase() != QuizSessionPhase.QUESTION_OPEN || session.getPhaseStartedAt() == null) {
            return false;
        }
        Instant deadline = session.getPhaseStartedAt().plusSeconds(question.durationSeconds());
        return !now.isBefore(deadline);
    }

    private LiveQuestion currentQuestion(QuizSession session) {
        int index = session.getCurrentQuestionIndex();
        if (index < 0) {
            throw new ConflictException("No active question for this session");
        }
        Long quizSetId = session.getQuizSet().getId();
        if (quizSetId == null) {
            List<QuizQuestion> questions = session.getQuizSet().getQuestions();
            if (index >= questions.size()) {
                throw new ConflictException("No active question for this session");
            }
            return LiveQuestion.from(questions.get(index));
        }
        QuizQuestionRepository.QuizLiveQuestionView question = questionRepository
                .findLiveByQuizSetIdAndPosition(quizSetId, index)
                .orElseThrow(() -> new ConflictException("No active question for this session"));
        return new LiveQuestion(
                question.getId(),
                question.getPosition(),
                question.getText(),
                question.getDurationSeconds(),
                question.getQuestionImagePresent(),
                question.getAnswerImagePresent(),
                answerRepository.findByQuestion_IdOrderByPositionAsc(question.getId()),
                null
        );
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
                questionCount(session),
                participantRepository.countBySession(session)
        );
    }

    private QuizSessionSnapshotResponse toSnapshot(QuizSession session, Long userId, boolean adminView) {
        LiveQuestion question = session.getCurrentQuestionIndex() < 0 ? null : currentQuestion(session);
        Instant serverTime = Instant.now();
        Instant questionEndsAt = session.getPhase() == QuizSessionPhase.QUESTION_OPEN
                && session.getPhaseStartedAt() != null
                && question != null
                ? session.getPhaseStartedAt().plusSeconds(question.durationSeconds())
                : null;
        boolean revealAnswers = adminView || session.getPhase() == QuizSessionPhase.ANSWER_REVEALED
                || session.getPhase() == QuizSessionPhase.SCOREBOARD
                || session.getPhase() == QuizSessionPhase.FINISHED;
        boolean revealAnswerImage = session.getPhase() == QuizSessionPhase.ANSWER_REVEALED
                || session.getPhase() == QuizSessionPhase.SCOREBOARD
                || session.getPhase() == QuizSessionPhase.FINISHED;
        Long selectedAnswerId = userId == null || question == null ? null : submissionRepository
                .findBySession_IdAndQuestion_IdAndUser_Id(session.getId(), question.id(), userId)
                .map(submission -> submission.getAnswer().getId())
                .orElse(null);
        Long correctAnswerId = revealAnswers && question != null ? question.answers().stream()
                .filter(QuizAnswer::isCorrect)
                .map(QuizAnswer::getId)
                .findFirst()
                .orElse(null) : null;
        boolean includeScores = adminView || session.getPhase() == QuizSessionPhase.SCOREBOARD
                || session.getPhase() == QuizSessionPhase.FINISHED;

        return new QuizSessionSnapshotResponse(
                session.getId(),
                session.getQuizSet().getId(),
                session.getQuizSet().getTitle(),
                session.getPhase(),
                session.getCurrentQuestionIndex(),
                questionCount(session),
                participantRepository.countBySession(session),
                serverTime,
                session.getPhaseStartedAt(),
                questionEndsAt,
                userId != null && participantRepository.existsBySession_IdAndUser_Id(session.getId(), userId),
                selectedAnswerId,
                correctAnswerId,
                question == null ? 0 : submissionRepository.countBySession_IdAndQuestion_Id(session.getId(), question.id()),
                question == null ? null : toLiveQuestionResponse(session.getId(), question, revealAnswers, revealAnswerImage),
                includeScores ? scores(session.getId()) : List.of()
        );
    }

    private QuizLiveQuestionResponse toLiveQuestionResponse(Long sessionId, LiveQuestion question,
                                                            boolean revealAnswers, boolean revealAnswerImage) {
        String imageBaseUrl = "/api/quizzes/sessions/" + sessionId + "/questions/" + question.id() + "/images/";
        return new QuizLiveQuestionResponse(
                question.id(),
                question.position(),
                question.text(),
                question.questionImagePresent() ? imageBaseUrl + "question" : null,
                revealAnswerImage && question.answerImagePresent() ? imageBaseUrl + "answer" : null,
                question.durationSeconds(),
                question.answers().stream().map(answer -> new QuizAnswerResponse(
                        answer.getId(),
                        answer.getPosition(),
                        answer.getText(),
                        revealAnswers ? answer.isCorrect() : null
                )).toList()
        );
    }

    private int questionCount(QuizSession session) {
        Long quizSetId = session.getQuizSet().getId();
        return quizSetId == null
                ? session.getQuizSet().getQuestions().size()
                : Math.toIntExact(questionRepository.countByQuizSet_Id(quizSetId));
    }

    private List<QuizScoreResponse> scores(Long sessionId) {
        return submissionRepository.findScoresBySessionId(sessionId).stream()
                .map(score -> new QuizScoreResponse(
                        score.getUserId(),
                        score.getDisplayName(),
                        Math.toIntExact(score.getScore())
                ))
                .toList();
    }

    private record LiveQuestion(Long id, int position, String text, int durationSeconds,
                                boolean questionImagePresent, boolean answerImagePresent,
                                List<QuizAnswer> answers, QuizQuestion entity) {

        private static LiveQuestion from(QuizQuestion question) {
            return new LiveQuestion(
                    question.getId(),
                    question.getPosition(),
                    question.getText(),
                    question.getDurationSeconds(),
                    question.getQuestionImageDataUrl() != null,
                    question.getAnswerImageDataUrl() != null,
                    question.getAnswers(),
                    question
            );
        }
    }
}
