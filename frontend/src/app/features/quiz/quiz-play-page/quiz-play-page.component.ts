import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { TuiButton, TuiTitle } from '@taiga-ui/core';
import { TuiCard, TuiHeader } from '@taiga-ui/layout';

import { QuizSessionSnapshotResponse, QuizSessionSummaryResponse } from '../models/quiz-api.model';
import { QuizApiService } from '../services/quiz-api.service';

@Component({
  selector: 'app-quiz-play-page',
  imports: [TuiButton, TuiCard, TuiHeader, TuiTitle],
  templateUrl: './quiz-play-page.component.html',
  styleUrl: './quiz-play-page.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuizPlayPageComponent implements OnInit, OnDestroy {
  private readonly quizApi = inject(QuizApiService);
  private refreshTimer: number | null = null;
  private clockTimer: number | null = null;

  readonly sessions = signal<readonly QuizSessionSummaryResponse[]>([]);
  readonly snapshot = signal<QuizSessionSnapshotResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly now = signal(Date.now());

  readonly remainingSeconds = computed(() => {
    const snapshot = this.snapshot();
    const question = snapshot?.currentQuestion;
    if (!snapshot?.phaseStartedAt || !question || snapshot.phase !== 'QUESTION_OPEN') {
      return null;
    }
    const startedAt = new Date(snapshot.phaseStartedAt).getTime();
    return Math.max(0, Math.ceil((startedAt + question.durationSeconds * 1000 - this.now()) / 1000));
  });

  ngOnInit(): void {
    void this.refresh();
    this.refreshTimer = window.setInterval(() => void this.refresh(), 2000);
    this.clockTimer = window.setInterval(() => this.now.set(Date.now()), 500);
  }

  ngOnDestroy(): void {
    if (this.refreshTimer !== null) window.clearInterval(this.refreshTimer);
    if (this.clockTimer !== null) window.clearInterval(this.clockTimer);
  }

  protected async selectSession(session: QuizSessionSummaryResponse): Promise<void> {
    await this.runAction(async () => {
      this.snapshot.set(await this.quizApi.findSession(session.id));
    });
  }

  protected async join(): Promise<void> {
    const snapshot = this.snapshot();
    if (!snapshot) return;
    await this.runAction(async () => {
      this.snapshot.set(await this.quizApi.joinSession(snapshot.id));
    });
  }

  protected async submitAnswer(answerId: number): Promise<void> {
    const snapshot = this.snapshot();
    if (!snapshot || snapshot.selectedAnswerId !== null) return;
    await this.runAction(async () => {
      this.snapshot.set(await this.quizApi.answer(snapshot.id, answerId));
    });
  }

  protected backToSessions(): void {
    this.snapshot.set(null);
  }

  protected phaseLabel(phase: string): string {
    const labels: Record<string, string> = {
      OPENING: 'Ouverture',
      QUESTION_OPEN: 'Question ouverte',
      QUESTION_LOCKED: 'Reponses fermees',
      ANSWER_REVEALED: 'Reponse affichee',
      SCOREBOARD: 'Scores',
      FINISHED: 'Termine'
    };

    return labels[phase] ?? phase;
  }

  private async refresh(): Promise<void> {
    try {
      const [sessions, snapshot] = await Promise.all([
        this.quizApi.findLiveSessions(),
        this.snapshot() === null ? Promise.resolve(null) : this.quizApi.findSession(this.snapshot()!.id)
      ]);
      this.sessions.set(sessions);
      if (snapshot !== null) {
        this.snapshot.set(snapshot);
      }
    } catch (error: unknown) {
      this.error.set(this.toErrorMessage(error));
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
    return 'Impossible de synchroniser le quiz pour le moment.';
  }
}
