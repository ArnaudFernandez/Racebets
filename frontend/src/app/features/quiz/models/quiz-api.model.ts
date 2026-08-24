export type QuizSessionPhase = 'OPENING' | 'QUESTION_OPEN' | 'QUESTION_LOCKED' | 'ANSWER_REVEALED' | 'SCOREBOARD' | 'FINISHED';

export interface QuizAnswerRequest {
  readonly text: string;
  readonly correct: boolean;
}

export interface QuizQuestionRequest {
  readonly text: string;
  readonly questionImageDataUrl: string | null;
  readonly answerImageDataUrl: string | null;
  readonly durationSeconds: number;
  readonly answers: readonly QuizAnswerRequest[];
}

export interface QuizSetRequest {
  readonly title: string;
  readonly questions: readonly QuizQuestionRequest[];
}

export interface QuizAnswerResponse {
  readonly id: number;
  readonly position: number;
  readonly text: string;
  readonly correct: boolean | null;
}

export interface QuizQuestionResponse {
  readonly id: number;
  readonly position: number;
  readonly text: string;
  readonly questionImageDataUrl: string | null;
  readonly answerImageDataUrl: string | null;
  readonly durationSeconds: number;
  readonly answers: readonly QuizAnswerResponse[];
}

export interface QuizSetListResponse {
  readonly id: number;
  readonly title: string;
  readonly status: 'CREATED';
  readonly questionCount: number;
}

export interface QuizSetDetailResponse extends QuizSetListResponse {
  readonly questions: readonly QuizQuestionResponse[];
}

export interface QuizSessionSummaryResponse {
  readonly id: number;
  readonly quizSetId: number;
  readonly title: string;
  readonly phase: QuizSessionPhase;
  readonly currentQuestionIndex: number;
  readonly questionCount: number;
  readonly participantCount: number;
}

export interface QuizScoreResponse {
  readonly userId: number;
  readonly displayName: string;
  readonly score: number;
}

export interface QuizSessionSnapshotResponse extends QuizSessionSummaryResponse {
  readonly serverTime?: string;
  readonly phaseStartedAt: string | null;
  readonly questionEndsAt?: string | null;
  readonly joined: boolean;
  readonly selectedAnswerId: number | null;
  readonly correctAnswerId: number | null;
  readonly submittedAnswers: number;
  readonly currentQuestion: QuizQuestionResponse | null;
  readonly scores: readonly QuizScoreResponse[];
}
