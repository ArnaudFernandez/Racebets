import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TuiTable } from '@taiga-ui/addon-table';
import { TuiButton, TuiLoader, TuiTitle } from '@taiga-ui/core';
import { TuiBadge } from '@taiga-ui/kit';
import { TuiCard, TuiHeader } from '@taiga-ui/layout';

import { QuizSessionPhase, QuizSessionSummaryResponse, QuizSetListResponse } from '../models/quiz-api.model';
import { QuizApiService } from '../services/quiz-api.service';

interface BackendErrorResponse {
  readonly message: string;
}

@Component({
  selector: 'app-admin-quiz-panel',
  imports: [TuiBadge, TuiButton, TuiCard, TuiHeader, TuiLoader, TuiTable, TuiTitle],
  templateUrl: './admin-quiz-panel.component.html',
  styleUrl: './admin-quiz-panel.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminQuizPanelComponent {
  private readonly quizApi = inject(QuizApiService);
  private readonly router = inject(Router);

  readonly quizSets = signal<readonly QuizSetListResponse[]>([]);
  readonly liveSessions = signal<readonly QuizSessionSummaryResponse[]>([]);
  readonly activeSession = computed(() => this.liveSessions()[0] ?? null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly deleteTarget = signal<QuizSetListResponse | null>(null);

  constructor() {
    void this.refresh();
  }

  protected createQuiz(): void {
    void this.router.navigate(['/admin/quizzes/new']);
  }

  protected editQuiz(quiz: QuizSetListResponse): void {
    void this.router.navigate(['/admin/quizzes', quiz.id, 'edit']);
  }

  protected controlSession(sessionId: number): void {
    void this.router.navigate(['/admin/quizzes/sessions', sessionId]);
  }

  protected isLiveQuiz(quizId: number): boolean {
    return this.activeSession()?.quizSetId === quizId;
  }

  protected phaseLabel(phase: QuizSessionPhase): string {
    return ({
      OPENING: 'Salle ouverte',
      QUESTION_OPEN: 'Réponses ouvertes',
      QUESTION_LOCKED: 'Réponses closes',
      ANSWER_REVEALED: 'Réponse révélée',
      SCOREBOARD: 'Classement',
      FINISHED: 'Terminée',
      CANCELLED: 'Arrêtée'
    })[phase];
  }

  protected async refresh(): Promise<void> {
    await this.run(async () => {
      const [quizSets, sessions] = await Promise.all([
        this.quizApi.findQuizSets(),
        this.quizApi.findAdminLiveSessions()
      ]);
      this.quizSets.set(quizSets);
      this.liveSessions.set(sessions);
    }, false);
  }

  protected async launch(quizId: number): Promise<void> {
    if (this.activeSession() !== null) return;
    await this.run(async () => {
      const session = await this.quizApi.openSession(quizId);
      await this.router.navigate(['/admin/quizzes/sessions', session.id]);
    });
  }

  protected requestDelete(quiz: QuizSetListResponse): void {
    this.deleteTarget.set(quiz);
  }

  protected cancelDelete(): void {
    this.deleteTarget.set(null);
  }

  protected async confirmDelete(): Promise<void> {
    const quiz = this.deleteTarget();
    if (quiz === null) return;
    this.deleteTarget.set(null);
    await this.run(async () => {
      await this.quizApi.deleteQuizSet(quiz.id);
      this.quizSets.set(await this.quizApi.findQuizSets());
      this.success.set('Questionnaire supprimé.');
    });
  }

  private async run(action: () => Promise<void>, clearSuccess = true): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    if (clearSuccess) this.success.set(null);
    try {
      await action();
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error === 'object' && error.error !== null) {
      const backend = error.error as BackendErrorResponse;
      if (typeof backend.message === 'string') return backend.message;
    }
    return 'Impossible de mettre à jour les quiz pour le moment.';
  }
}
