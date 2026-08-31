import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TuiTable } from '@taiga-ui/addon-table';
import { TuiButton, TuiDialog, TuiInput, TuiLoader, TuiTitle } from '@taiga-ui/core';
import { TuiBadge } from '@taiga-ui/kit';
import { TuiCard, TuiHeader } from '@taiga-ui/layout';
import { EMPTY, catchError, exhaustMap, from, tap, timer } from 'rxjs';

import { WordCloudQuestionListItem, WordCloudQuestionStatus } from '../models/word-cloud.model';
import { WordCloudApiService } from '../services/word-cloud-api.service';

type ConfirmationAction = 'delete' | 'reset';

interface Confirmation {
  readonly action: ConfirmationAction;
  readonly question: WordCloudQuestionListItem;
}

@Component({
  selector: 'app-admin-word-cloud-panel',
  imports: [ReactiveFormsModule, TuiBadge, TuiButton, TuiCard, TuiDialog, TuiHeader, TuiInput, TuiLoader, TuiTable, TuiTitle],
  templateUrl: './admin-word-cloud-panel.component.html',
  styleUrl: './admin-word-cloud-panel.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminWordCloudPanelComponent {
  private readonly api = inject(WordCloudApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly questions = signal<readonly WordCloudQuestionListItem[]>([]);
  readonly initialLoading = signal(true);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly editingId = signal<number | null>(null);
  readonly editorDialogOpen = signal(false);
  readonly confirmation = signal<Confirmation | null>(null);
  readonly questionForm = new FormGroup({
    text: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(300)]
    })
  });

  constructor() {
    timer(0, 1500)
      .pipe(
        exhaustMap(() => this.busy() || this.editorDialogOpen() || this.confirmation() !== null
          ? EMPTY
          : from(this.refresh()).pipe(
              tap(() => this.error.set(null)),
              catchError((error: unknown) => {
                this.error.set(this.errorMessage(error));
                return EMPTY;
              })
            )),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  protected statusLabel(status: WordCloudQuestionStatus): string {
    return ({ DRAFT: 'Brouillon', OPEN: 'Réponses ouvertes', REVEALED: 'Nuage révélé', CLOSED: 'Terminée' })[status];
  }

  protected statusAppearance(status: WordCloudQuestionStatus): string {
    return ({ DRAFT: 'neutral', OPEN: 'positive', REVEALED: 'info', CLOSED: 'neutral' })[status];
  }

  protected create(): void {
    this.editingId.set(null);
    this.questionForm.reset({ text: '' });
    this.editorDialogOpen.set(true);
    this.clearMessages();
  }

  protected edit(question: WordCloudQuestionListItem): void {
    if (question.status !== 'DRAFT') return;
    this.editingId.set(question.id);
    this.questionForm.setValue({ text: question.text });
    this.editorDialogOpen.set(true);
    this.clearMessages();
  }

  protected closeEditor(): void {
    this.editorDialogOpen.set(false);
    this.editingId.set(null);
    this.questionForm.reset({ text: '' });
  }

  protected editorDialogChanged(open: boolean): void {
    if (!open) this.closeEditor();
  }

  protected async save(): Promise<void> {
    this.questionForm.markAllAsTouched();
    if (this.questionForm.invalid || this.busy()) return;
    const text = this.questionForm.controls.text.value.trim();
    if (text.length === 0) return;
    const editingId = this.editingId();

    await this.run(async () => {
      if (editingId === null) {
        await this.api.createQuestion({ text });
        this.success.set('Question ajoutée aux brouillons.');
      } else {
        await this.api.updateQuestion(editingId, { text });
        this.success.set('Brouillon mis à jour.');
      }
      this.closeEditor();
      await this.refresh();
    });
  }

  protected control(question: WordCloudQuestionListItem): void {
    void this.router.navigate(['/admin/word-cloud/questions', question.id]);
  }

  protected request(action: ConfirmationAction, question: WordCloudQuestionListItem): void {
    this.confirmation.set({ action, question });
    this.clearMessages();
  }

  protected cancelConfirmation(): void {
    this.confirmation.set(null);
  }

  protected confirmationDialogChanged(open: boolean): void {
    if (!open) this.cancelConfirmation();
  }

  protected async confirm(): Promise<void> {
    const pending = this.confirmation();
    if (pending === null || this.busy()) return;

    await this.run(async () => {
      if (pending.action === 'delete') {
        await this.api.deleteQuestion(pending.question.id);
        this.success.set('Brouillon supprimé.');
      } else {
        await this.api.resetQuestion(pending.question.id);
        this.success.set('Question réinitialisée. Toutes ses réponses ont été supprimées.');
      }
      this.confirmation.set(null);
      await this.refresh();
    });
  }

  private async refresh(): Promise<void> {
    try {
      this.questions.set(await this.api.findQuestions());
    } finally {
      this.initialLoading.set(false);
    }
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.busy.set(true);
    this.clearMessages();
    try {
      await action();
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  private clearMessages(): void {
    this.error.set(null);
    this.success.set(null);
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') return error.error.message;
    return 'Impossible de synchroniser le nuage de mots pour le moment.';
  }
}
