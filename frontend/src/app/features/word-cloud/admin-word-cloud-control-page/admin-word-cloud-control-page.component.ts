import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TuiTable } from '@taiga-ui/addon-table';
import { TuiButton, TuiDialog, TuiLoader } from '@taiga-ui/core';
import { TuiBadge } from '@taiga-ui/kit';

import {
  WordCloudAdminResponse,
  WordCloudAdminSnapshot,
  WordCloudQuestionStatus
} from '../models/word-cloud.model';
import { presentWord } from '../word-cloud-presentation';
import { WordCloudApiService } from '../services/word-cloud-api.service';

type ControlAction = 'open' | 'reveal' | 'close' | 'reset' | 'censor';

interface PendingAction {
  readonly action: ControlAction;
  readonly response?: WordCloudAdminResponse;
}

const STATUS_ORDER: readonly WordCloudQuestionStatus[] = ['DRAFT', 'OPEN', 'REVEALED', 'CLOSED'];
const REFRESH_ERROR = 'Impossible d’actualiser les réponses pour le moment.';

@Component({
  selector: 'app-admin-word-cloud-control-page',
  imports: [RouterLink, TuiBadge, TuiButton, TuiDialog, TuiLoader, TuiTable],
  templateUrl: './admin-word-cloud-control-page.component.html',
  styleUrl: './admin-word-cloud-control-page.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminWordCloudControlPageComponent implements OnDestroy {
  private readonly api = inject(WordCloudApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly questionId = Number(this.route.snapshot.paramMap.get('questionId'));
  private readonly pollId: number;
  private refreshing = false;
  private requestVersion = 0;

  readonly statuses = STATUS_ORDER;
  readonly question = signal<WordCloudAdminSnapshot | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly pendingAction = signal<PendingAction | null>(null);
  readonly statusIndex = computed(() => STATUS_ORDER.indexOf(this.question()?.status ?? 'DRAFT'));
  readonly visibleResponseCount = computed(() => this.question()?.responses.filter((response) => !response.censored).length ?? 0);
  readonly cloudWords = computed(() => {
    const responses = this.question()?.responses.filter((response) => !response.censored) ?? [];
    const maxCount = Math.max(1, ...responses.map((response) => response.count));
    return responses.map((response) => presentWord(response, maxCount));
  });

  constructor() {
    void this.load();
    this.pollId = window.setInterval(() => {
      if (!this.loading() && this.pendingAction() === null) void this.refresh();
    }, 1500);
  }

  ngOnDestroy(): void {
    window.clearInterval(this.pollId);
  }

  protected statusLabel(status: WordCloudQuestionStatus): string {
    return ({ DRAFT: 'Brouillon', OPEN: 'Réponses ouvertes', REVEALED: 'Nuage révélé', CLOSED: 'Terminée' })[status];
  }

  protected statusAppearance(status: WordCloudQuestionStatus): string {
    return ({ DRAFT: 'neutral', OPEN: 'positive', REVEALED: 'info', CLOSED: 'neutral' })[status];
  }

  protected request(action: ControlAction, response?: WordCloudAdminResponse): void {
    this.pendingAction.set({ action, response });
    this.error.set(null);
    this.success.set(null);
  }

  protected cancelAction(): void {
    this.pendingAction.set(null);
  }

  protected dialogChanged(open: boolean): void {
    if (!open) this.cancelAction();
  }

  protected dialogTitle(action: ControlAction): string {
    return ({
      open: 'Ouvrir les réponses ?',
      reveal: 'Révéler le nuage ?',
      close: 'Terminer cette question ?',
      reset: 'Réinitialiser cette question ?',
      censor: 'Censurer cette réponse ?'
    })[action];
  }

  protected dialogCopy(action: ControlAction): string {
    return ({
      open: 'La question sera affichée et tous les participants pourront répondre.',
      reveal: 'Le nuage filtré deviendra immédiatement visible par tous les participants.',
      close: 'La question quittera l’écran live. Les réponses resteront consultables dans l’administration.',
      reset: 'La question reviendra à l’état brouillon. Toutes les réponses et les censures seront définitivement supprimées.',
      censor: 'Cette réponse et toutes ses variantes de casse ou d’accent seront retirées du nuage de mots.'
    })[action];
  }

  protected dialogButton(action: ControlAction): string {
    return ({
      open: 'Ouvrir les réponses',
      reveal: 'Révéler maintenant',
      close: 'Terminer la question',
      reset: 'Réinitialiser et supprimer les réponses',
      censor: 'Censurer la réponse'
    })[action];
  }

  protected async confirmAction(): Promise<void> {
    const pending = this.pendingAction();
    if (pending === null || this.loading()) return;
    this.pendingAction.set(null);

    await this.run(async () => {
      if (pending.action === 'open') await this.api.openQuestion(this.questionId);
      if (pending.action === 'reveal') await this.api.revealQuestion(this.questionId);
      if (pending.action === 'close') await this.api.closeQuestion(this.questionId);
      if (pending.action === 'reset') await this.api.resetQuestion(this.questionId);
      if (pending.action === 'censor' && pending.response !== undefined) {
        this.question.set(await this.api.censorResponse(this.questionId, pending.response.text));
      } else {
        this.question.set(await this.api.findAdminQuestion(this.questionId));
      }
      this.success.set(({
        open: 'Les réponses sont ouvertes.',
        reveal: 'Le nuage est visible par les participants.',
        close: 'La question est terminée.',
        reset: 'La question a été réinitialisée et ses réponses supprimées.',
        censor: 'La réponse a été retirée du nuage.'
      })[pending.action]);
    });
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      this.question.set(await this.api.findAdminQuestion(this.questionId));
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private async refresh(): Promise<void> {
    if (this.refreshing) return;
    this.refreshing = true;
    const version = this.requestVersion;
    try {
      const question = await this.api.findAdminQuestion(this.questionId);
      if (version !== this.requestVersion || this.pendingAction() !== null) return;
      this.question.set(question);
      if (this.error() === REFRESH_ERROR) this.error.set(null);
    } catch {
      if (version === this.requestVersion) this.error.set(REFRESH_ERROR);
    } finally {
      this.refreshing = false;
    }
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.requestVersion++;
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

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') return error.error.message;
    return 'Impossible de mettre à jour cette question pour le moment.';
  }
}
