import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Injector,
  OnDestroy,
  OnInit,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChild
} from '@angular/core';
import { TuiButton, TuiDialog, TuiIcon, TuiLoader } from '@taiga-ui/core';
import { TuiAvatar, TuiInitialsPipe, TuiProgress } from '@taiga-ui/kit';

import { AuthService } from '../../../core/auth/auth.service';
import { PublicPartner } from '../../betting/models/partner.model';
import { PartnerService } from '../../betting/services/partner.service';
import {
  QuizAnswerResponse,
  QuizScoreResponse,
  QuizSessionPhase,
  QuizSessionSnapshotResponse,
  QuizSessionSummaryResponse
} from '../models/quiz-api.model';
import { QuizApiService } from '../services/quiz-api.service';

@Component({
  selector: 'app-quiz-play-page',
  imports: [TuiAvatar, TuiButton, TuiDialog, TuiIcon, TuiInitialsPipe, TuiLoader, TuiProgress],
  templateUrl: './quiz-play-page.component.html',
  styleUrls: ['./quiz-play-page.component.less', './quiz-podium.component.less'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuizPlayPageComponent implements OnInit, OnDestroy {
  private readonly quizApi = inject(QuizApiService);
  private readonly auth = inject(AuthService);
  private readonly partnerService = inject(PartnerService);
  private readonly injector = inject(Injector);
  private readonly scoreboardHeading =
    viewChild<ElementRef<HTMLHeadingElement>>('scoreboardHeading');
  private readonly podiumHeading = viewChild<ElementRef<HTMLHeadingElement>>('podiumHeading');
  private refreshTimer: number | null = null;
  private clockTimer: number | null = null;
  private suppressedSessionId: number | null = null;
  private refreshing = false;

  readonly sessions = signal<readonly QuizSessionSummaryResponse[]>([]);
  readonly snapshot = signal<QuizSessionSnapshotResponse | null>(null);
  readonly loading = signal(false);
  readonly answeringId = signal<number | null>(null);
  readonly optimisticAnswerId = signal<number | null>(null);
  readonly answerChangeCandidate = signal<QuizAnswerResponse | null>(null);
  readonly error = signal<string | null>(null);
  readonly now = signal(Date.now());
  readonly serverClockOffset = signal(0);
  readonly partners = signal<readonly PublicPartner[]>([]);
  readonly podiumWinners = computed(() => {
    const winners = (this.snapshot()?.scores ?? []).slice(0, 3).map((score, index) => ({
      score,
      rank: index + 1
    }));

    return [winners[1], winners[0], winners[2]].filter(
      (winner): winner is { score: QuizScoreResponse; rank: number } => winner !== undefined
    );
  });

  readonly remainingMilliseconds = computed(() => {
    const snapshot = this.snapshot();
    if (snapshot?.phase !== 'QUESTION_OPEN') return null;
    const explicitDeadline = Date.parse(snapshot.questionEndsAt ?? '');
    const phaseStartedAt = Date.parse(snapshot.phaseStartedAt ?? '');
    const fallbackDeadline =
      phaseStartedAt + (snapshot.currentQuestion?.durationSeconds ?? 0) * 1000;
    const deadline = Number.isFinite(explicitDeadline) ? explicitDeadline : fallbackDeadline;
    if (!Number.isFinite(deadline)) return null;
    const serverNow = this.now() + this.serverClockOffset();
    return Math.max(0, deadline - serverNow);
  });
  readonly remainingSeconds = computed(() => {
    const remaining = this.remainingMilliseconds();
    return remaining === null ? null : Math.ceil(remaining / 1000);
  });
  readonly timerProgress = computed(() => {
    const remaining = this.remainingMilliseconds();
    const duration = this.snapshot()?.currentQuestion?.durationSeconds;
    if (remaining === null || duration === undefined) return 0;
    return Math.max(0, Math.min(100, (remaining / (duration * 1000)) * 100));
  });
  readonly timeExpired = computed(() => this.remainingMilliseconds() === 0);
  readonly displayedAnswerId = computed(
    () => this.optimisticAnswerId() ?? this.snapshot()?.selectedAnswerId ?? null
  );
  readonly currentUserId = computed(() => this.auth.user()?.id ?? null);
  readonly correctAnswer = computed(() => {
    const snapshot = this.snapshot();
    return (
      snapshot?.currentQuestion?.answers.find((answer) => answer.id === snapshot.correctAnswerId) ??
      null
    );
  });
  readonly selectedAnswer = computed(() => {
    const snapshot = this.snapshot();
    return (
      snapshot?.currentQuestion?.answers.find(
        (answer) => answer.id === snapshot.selectedAnswerId
      ) ?? null
    );
  });
  readonly correctAnswerIndex = computed(() => {
    const snapshot = this.snapshot();
    return (
      snapshot?.currentQuestion?.answers.findIndex(
        (answer) => answer.id === snapshot.correctAnswerId
      ) ?? -1
    );
  });
  readonly selectedAnswerIndex = computed(() => {
    const snapshot = this.snapshot();
    return (
      snapshot?.currentQuestion?.answers.findIndex(
        (answer) => answer.id === snapshot.selectedAnswerId
      ) ?? -1
    );
  });
  readonly answeredCorrectly = computed(() => {
    const snapshot = this.snapshot();
    return (
      snapshot?.selectedAnswerId !== null &&
      snapshot?.selectedAnswerId === snapshot?.correctAnswerId
    );
  });
  readonly currentPlayerScore = computed(() => {
    const userId = this.currentUserId();
    return userId === null
      ? null
      : (this.snapshot()?.scores.find((score) => score.userId === userId) ?? null);
  });
  readonly currentPlayerRank = computed(() => {
    const userId = this.currentUserId();
    if (userId === null) return null;
    const index = this.snapshot()?.scores.findIndex((score) => score.userId === userId) ?? -1;
    return index < 0 ? null : index + 1;
  });

  ngOnInit(): void {
    void this.refresh();
    void this.loadPartners();
    this.refreshTimer = window.setInterval(() => void this.refresh(), 1000);
    this.clockTimer = window.setInterval(() => this.now.set(Date.now()), 100);
  }

  ngOnDestroy(): void {
    if (this.refreshTimer !== null) window.clearInterval(this.refreshTimer);
    if (this.clockTimer !== null) window.clearInterval(this.clockTimer);
  }

  protected async selectSession(session: QuizSessionSummaryResponse): Promise<void> {
    this.suppressedSessionId = null;
    await this.runAction(async () =>
      this.applySnapshot(await this.quizApi.findSession(session.id))
    );
  }

  protected async join(): Promise<void> {
    const snapshot = this.snapshot();
    if (snapshot === null) return;
    await this.runAction(async () =>
      this.applySnapshot(await this.quizApi.joinSession(snapshot.id))
    );
  }

  protected selectAnswer(answerId: number): void {
    const snapshot = this.snapshot();
    if (
      snapshot?.phase !== 'QUESTION_OPEN' ||
      this.timeExpired() ||
      this.answeringId() !== null ||
      this.displayedAnswerId() === answerId
    )
      return;

    const answer = snapshot.currentQuestion?.answers.find((item) => item.id === answerId);
    if (answer === undefined) return;

    if (this.displayedAnswerId() !== null) {
      this.answerChangeCandidate.set(answer);
      return;
    }

    void this.submitAnswer(answerId);
  }

  protected cancelAnswerChange(): void {
    this.answerChangeCandidate.set(null);
  }

  protected answerChangeDialogOpenChange(open: boolean): void {
    if (!open) this.cancelAnswerChange();
  }

  protected confirmAnswerChange(): void {
    const candidate = this.answerChangeCandidate();
    this.answerChangeCandidate.set(null);
    if (candidate !== null) void this.submitAnswer(candidate.id);
  }

  private async submitAnswer(answerId: number): Promise<void> {
    const snapshot = this.snapshot();
    if (
      snapshot?.phase !== 'QUESTION_OPEN' ||
      this.timeExpired() ||
      this.answeringId() !== null ||
      this.displayedAnswerId() === answerId
    )
      return;

    this.optimisticAnswerId.set(answerId);
    this.answeringId.set(answerId);
    this.error.set(null);
    try {
      this.applySnapshot(await this.quizApi.answer(snapshot.id, answerId));
    } catch (error: unknown) {
      this.optimisticAnswerId.set(null);
      this.error.set(this.toErrorMessage(error));
      await this.refresh();
    } finally {
      this.answeringId.set(null);
    }
  }

  protected backToSessions(): void {
    this.suppressedSessionId = this.snapshot()?.id ?? null;
    this.snapshot.set(null);
    this.optimisticAnswerId.set(null);
    this.answerChangeCandidate.set(null);
  }

  protected phaseLabel(phase: QuizSessionPhase): string {
    return {
      OPENING: 'Salle ouverte',
      QUESTION_OPEN: 'Question en cours',
      QUESTION_LOCKED: 'Réponses terminées',
      ANSWER_REVEALED: 'Correction',
      SCOREBOARD: 'Classement',
      FINISHED: 'Terminé',
      CANCELLED: 'Arrêté'
    }[phase];
  }

  protected answerLetter(index: number): string {
    return ['A', 'B', 'C', 'D'][index] ?? String(index + 1);
  }

  protected rankLabel(rank: number): string {
    return rank === 1 ? '1er' : `${rank}e`;
  }

  private async refresh(): Promise<void> {
    if (this.refreshing) return;
    this.refreshing = true;
    try {
      const activeSnapshot = this.snapshot();
      const [sessions, refreshedSnapshot] = await Promise.all([
        this.quizApi.findLiveSessions(),
        activeSnapshot === null
          ? Promise.resolve(null)
          : this.quizApi.findSession(activeSnapshot.id)
      ]);
      this.sessions.set(sessions);
      if (refreshedSnapshot !== null) {
        if (this.answeringId() === null) this.applySnapshot(refreshedSnapshot);
      } else if (sessions.length === 1 && sessions[0].id !== this.suppressedSessionId) {
        this.applySnapshot(await this.quizApi.findSession(sessions[0].id));
      }
      this.error.set(null);
    } catch (error: unknown) {
      this.error.set(this.toErrorMessage(error));
    } finally {
      this.refreshing = false;
    }
  }

  private applySnapshot(snapshot: QuizSessionSnapshotResponse): void {
    const previousPhase = this.snapshot()?.phase;
    const serverTime = Date.parse(snapshot.serverTime ?? '');
    this.serverClockOffset.set(Number.isFinite(serverTime) ? serverTime - Date.now() : 0);
    this.now.set(Date.now());
    this.optimisticAnswerId.set(null);
    if (snapshot.phase !== 'QUESTION_OPEN') this.answerChangeCandidate.set(null);
    if (snapshot.phase === 'CANCELLED') {
      this.suppressedSessionId = snapshot.id;
      this.sessions.update((sessions) => sessions.filter((session) => session.id !== snapshot.id));
      this.snapshot.set(null);
      this.answeringId.set(null);
      return;
    }
    this.snapshot.set(snapshot);
    if (snapshot.phase === 'SCOREBOARD' && previousPhase !== 'SCOREBOARD') {
      afterNextRender(() => this.scoreboardHeading()?.nativeElement.focus(), {
        injector: this.injector
      });
    }
    if (snapshot.phase === 'FINISHED' && previousPhase !== 'FINISHED') {
      afterNextRender(() => this.podiumHeading()?.nativeElement.focus(), {
        injector: this.injector
      });
    }
  }

  private async loadPartners(): Promise<void> {
    try {
      this.partners.set(await this.partnerService.findVisible());
    } catch {
      this.partners.set([]);
    }
  }

  private async runAction(action: () => Promise<void>): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      await action();
    } catch (error: unknown) {
      this.error.set(this.toErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private toErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403)) {
      return 'Connectez-vous pour participer au quiz.';
    }
    if (error instanceof HttpErrorResponse && error.status === 409) {
      return 'Le temps est écoulé : cette réponse ne peut plus être enregistrée.';
    }
    return 'La synchronisation a été interrompue. Une nouvelle tentative est en cours.';
  }
}
