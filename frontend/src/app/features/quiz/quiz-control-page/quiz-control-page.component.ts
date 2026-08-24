import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TuiButton, TuiDialog, TuiLoader } from '@taiga-ui/core';
import { TuiBadge } from '@taiga-ui/kit';

import { QuizAnswerResponse, QuizSessionPhase, QuizSessionSnapshotResponse } from '../models/quiz-api.model';
import { QuizApiService } from '../services/quiz-api.service';

interface BackendErrorResponse {
  readonly message: string;
}

@Component({
  selector: 'app-quiz-control-page',
  imports: [RouterLink, TuiBadge, TuiButton, TuiDialog, TuiLoader],
  templateUrl: './quiz-control-page.component.html',
  styleUrl: './quiz-control-page.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuizControlPageComponent implements OnDestroy {
  private readonly quizApi = inject(QuizApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly sessionId = Number(this.route.snapshot.paramMap.get('sessionId'));
  private readonly pollId: number;
  private readonly clockId: number;

  readonly session = signal<QuizSessionSnapshotResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly earlyStopDialogOpen = signal(false);
  readonly now = signal(Date.now());
  readonly serverClockOffset = signal(0);
  readonly remainingSeconds = computed(() => {
    const session = this.session();
    if (session?.phase !== 'QUESTION_OPEN') return 0;
    const explicitDeadline = Date.parse(session.questionEndsAt ?? '');
    const phaseStartedAt = Date.parse(session.phaseStartedAt ?? '');
    const fallbackDeadline = phaseStartedAt + (session.currentQuestion?.durationSeconds ?? 0) * 1000;
    const deadline = Number.isFinite(explicitDeadline) ? explicitDeadline : fallbackDeadline;
    if (!Number.isFinite(deadline)) return 0;
    const remaining = deadline - (this.now() + this.serverClockOffset());
    return Math.max(0, Math.ceil(remaining / 1000));
  });
  readonly responseRate = computed(() => {
    const session = this.session();
    if (session === null || session.participantCount === 0) return 0;
    return Math.min(100, Math.round(session.submittedAnswers / session.participantCount * 100));
  });

  readonly phases: readonly QuizSessionPhase[] = ['OPENING', 'QUESTION_OPEN', 'QUESTION_LOCKED', 'ANSWER_REVEALED', 'SCOREBOARD', 'FINISHED'];

  constructor() {
    void this.refresh();
    this.pollId = window.setInterval(() => {
      if (!this.loading()) void this.refresh(false);
    }, 1500);
    this.clockId = window.setInterval(() => this.now.set(Date.now()), 250);
  }

  ngOnDestroy(): void {
    window.clearInterval(this.pollId);
    window.clearInterval(this.clockId);
  }

  protected phaseLabel(phase: QuizSessionPhase): string {
    return ({
      OPENING: 'Salle ouverte',
      QUESTION_OPEN: 'Réponses ouvertes',
      QUESTION_LOCKED: 'Réponses closes',
      ANSWER_REVEALED: 'Correction',
      SCOREBOARD: 'Classement',
      FINISHED: 'Terminé'
    })[phase];
  }

  protected phaseIndex(phase: QuizSessionPhase): number {
    return this.phases.indexOf(phase);
  }

  protected nextActionLabel(session: QuizSessionSnapshotResponse): string {
    return ({
      OPENING: 'Lancer la première question',
      QUESTION_OPEN: 'Arrêter la question',
      QUESTION_LOCKED: 'Révéler la bonne réponse',
      ANSWER_REVEALED: 'Afficher le classement',
      SCOREBOARD: session.currentQuestionIndex + 1 >= session.questionCount ? 'Terminer le quiz' : 'Question suivante',
      FINISHED: ''
    })[session.phase];
  }

  protected nextActionHint(phase: QuizSessionPhase): string {
    return ({
      OPENING: 'Les participants présents entreront dans le quiz.',
      QUESTION_OPEN: 'La question s’arrêtera automatiquement à zéro, ou dès votre confirmation.',
      QUESTION_LOCKED: 'La correction sera affichée à tous les participants.',
      ANSWER_REVEALED: 'Le classement actualisé apparaîtra sur les écrans.',
      SCOREBOARD: 'Le déroulé continuera vers la prochaine question.',
      FINISHED: ''
    })[phase];
  }

  protected answerLetter(index: number): string {
    return ['A', 'B', 'C', 'D'][index] ?? String(index + 1);
  }

  protected isCorrect(answer: QuizAnswerResponse, session: QuizSessionSnapshotResponse): boolean {
    return answer.id === session.correctAnswerId;
  }

  protected async advance(): Promise<void> {
    const session = this.session();
    if (session === null || session.phase === 'QUESTION_OPEN' || session.phase === 'FINISHED') return;
    await this.run(async () => {
      const updated = await this.transition(session);
      this.applySession(updated);
      this.success.set(this.transitionMessage(updated.phase));
    });
  }

  protected requestEarlyStop(): void {
    if (this.session()?.phase !== 'QUESTION_OPEN' || this.remainingSeconds() === 0) return;
    this.earlyStopDialogOpen.set(true);
  }

  protected cancelEarlyStop(): void {
    this.earlyStopDialogOpen.set(false);
  }

  protected async confirmEarlyStop(): Promise<void> {
    const session = this.session();
    if (session?.phase !== 'QUESTION_OPEN') {
      this.earlyStopDialogOpen.set(false);
      return;
    }
    this.earlyStopDialogOpen.set(false);
    await this.run(async () => {
      this.applySession(await this.quizApi.lockQuestion(session.id));
      this.success.set('La question a été arrêtée. Les réponses sont verrouillées.');
    });
  }

  protected async refresh(showError = true): Promise<void> {
    try {
      this.applySession(await this.quizApi.findAdminSession(this.sessionId));
      if (showError) this.error.set(null);
    } catch (error: unknown) {
      if (showError) this.error.set(this.errorMessage(error));
    }
  }

  private transition(session: QuizSessionSnapshotResponse): Promise<QuizSessionSnapshotResponse> {
    switch (session.phase) {
      case 'OPENING': return this.quizApi.startSession(session.id);
      case 'QUESTION_OPEN': return this.quizApi.lockQuestion(session.id);
      case 'QUESTION_LOCKED': return this.quizApi.revealAnswer(session.id);
      case 'ANSWER_REVEALED': return this.quizApi.showScoreboard(session.id);
      case 'SCOREBOARD': return this.quizApi.nextQuestion(session.id);
      case 'FINISHED': return Promise.resolve(session);
    }
  }

  private applySession(session: QuizSessionSnapshotResponse): void {
    const serverTime = Date.parse(session.serverTime ?? '');
    this.serverClockOffset.set(Number.isFinite(serverTime) ? serverTime - Date.now() : 0);
    this.now.set(Date.now());
    this.session.set(session);
    if (session.phase !== 'QUESTION_OPEN') this.earlyStopDialogOpen.set(false);
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    this.success.set(null);
    try {
      await action();
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private transitionMessage(phase: QuizSessionPhase): string {
    return ({
      OPENING: '',
      QUESTION_OPEN: 'La question est maintenant visible.',
      QUESTION_LOCKED: 'Les réponses sont verrouillées.',
      ANSWER_REVEALED: 'La bonne réponse est révélée.',
      SCOREBOARD: 'Le classement est affiché.',
      FINISHED: 'Le quiz est terminé. Une nouvelle session peut désormais être lancée.'
    })[phase];
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error === 'object' && error.error !== null) {
      const backend = error.error as BackendErrorResponse;
      if (typeof backend.message === 'string') return backend.message;
    }
    return 'Impossible de mettre à jour la session pour le moment.';
  }
}
