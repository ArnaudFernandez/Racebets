import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TuiButton, TuiLoader } from '@taiga-ui/core';

import { AppFeaturesService } from '../../../core/features/app-features.service';
import { QuizAnswerResponse, QuizSessionPhase, QuizSessionSnapshotResponse, QuizSessionSummaryResponse } from '../models/quiz-api.model';
import { QuizApiService } from '../services/quiz-api.service';

@Component({
  selector: 'app-quiz-play-page',
  imports: [TuiButton, TuiLoader],
  templateUrl: './quiz-play-page.component.html',
  styleUrl: './quiz-play-page.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuizPlayPageComponent implements OnInit, OnDestroy {
  private readonly quizApi = inject(QuizApiService);
  private readonly features = inject(AppFeaturesService);
  private readonly router = inject(Router);
  private refreshTimer: number | null = null;
  private clockTimer: number | null = null;
  private suppressedSessionId: number | null = null;
  private completionHandled = false;

  readonly sessions = signal<readonly QuizSessionSummaryResponse[]>([]);
  readonly snapshot = signal<QuizSessionSnapshotResponse | null>(null);
  readonly loading = signal(false);
  readonly answeringId = signal<number | null>(null);
  readonly optimisticAnswerId = signal<number | null>(null);
  readonly error = signal<string | null>(null);
  readonly now = signal(Date.now());
  readonly serverClockOffset = signal(0);

  readonly remainingMilliseconds = computed(() => {
    const snapshot = this.snapshot();
    if (snapshot?.phase !== 'QUESTION_OPEN') return null;
    const explicitDeadline = Date.parse(snapshot.questionEndsAt ?? '');
    const phaseStartedAt = Date.parse(snapshot.phaseStartedAt ?? '');
    const fallbackDeadline = phaseStartedAt + (snapshot.currentQuestion?.durationSeconds ?? 0) * 1000;
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
    return Math.max(0, Math.min(100, remaining / (duration * 1000) * 100));
  });
  readonly timeExpired = computed(() => this.remainingMilliseconds() === 0);
  readonly displayedAnswerId = computed(() => this.optimisticAnswerId() ?? this.snapshot()?.selectedAnswerId ?? null);

  ngOnInit(): void {
    void this.refresh();
    this.refreshTimer = window.setInterval(() => void this.refresh(), 1000);
    this.clockTimer = window.setInterval(() => this.now.set(Date.now()), 100);
  }

  ngOnDestroy(): void {
    if (this.refreshTimer !== null) window.clearInterval(this.refreshTimer);
    if (this.clockTimer !== null) window.clearInterval(this.clockTimer);
  }

  protected async selectSession(session: QuizSessionSummaryResponse): Promise<void> {
    this.suppressedSessionId = null;
    await this.runAction(async () => this.applySnapshot(await this.quizApi.findSession(session.id)));
  }

  protected async join(): Promise<void> {
    const snapshot = this.snapshot();
    if (snapshot === null) return;
    await this.runAction(async () => this.applySnapshot(await this.quizApi.joinSession(snapshot.id)));
  }

  protected async submitAnswer(answerId: number): Promise<void> {
    const snapshot = this.snapshot();
    if (
      snapshot?.phase !== 'QUESTION_OPEN'
      || this.timeExpired()
      || this.answeringId() !== null
      || this.displayedAnswerId() === answerId
    ) return;

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
  }

  protected phaseLabel(phase: QuizSessionPhase): string {
    return ({
      OPENING: 'Salle ouverte',
      QUESTION_OPEN: 'Question en cours',
      QUESTION_LOCKED: 'Réponses terminées',
      ANSWER_REVEALED: 'Correction',
      SCOREBOARD: 'Classement',
      FINISHED: 'Terminé'
    })[phase];
  }

  protected answerLetter(index: number): string {
    return ['A', 'B', 'C', 'D'][index] ?? String(index + 1);
  }

  protected isCorrect(answer: QuizAnswerResponse, session: QuizSessionSnapshotResponse): boolean {
    return answer.id === session.correctAnswerId;
  }

  private async refresh(): Promise<void> {
    try {
      const activeSnapshot = this.snapshot();
      const [sessions, refreshedSnapshot] = await Promise.all([
        this.quizApi.findLiveSessions(),
        activeSnapshot === null ? Promise.resolve(null) : this.quizApi.findSession(activeSnapshot.id)
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
    }
  }

  private applySnapshot(snapshot: QuizSessionSnapshotResponse): void {
    const serverTime = Date.parse(snapshot.serverTime ?? '');
    this.serverClockOffset.set(Number.isFinite(serverTime) ? serverTime - Date.now() : 0);
    this.now.set(Date.now());
    this.optimisticAnswerId.set(null);
    if (snapshot.phase === 'FINISHED') {
      void this.leaveCompletedSession();
      return;
    }
    this.completionHandled = false;
    this.snapshot.set(snapshot);
  }

  private async leaveCompletedSession(): Promise<void> {
    if (this.completionHandled) return;
    this.completionHandled = true;
    this.snapshot.set(null);
    this.sessions.set([]);
    const settings = await this.features.ensureLoaded();
    const destination = this.features.defaultPath(settings);
    if (destination !== '/quiz') await this.router.navigateByUrl(destination);
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
