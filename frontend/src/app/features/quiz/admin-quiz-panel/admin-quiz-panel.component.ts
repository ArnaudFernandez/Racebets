import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TuiButton, TuiInput, TuiTitle } from '@taiga-ui/core';
import { TuiCard, TuiHeader } from '@taiga-ui/layout';
import { TuiBadge } from '@taiga-ui/kit';

import {
  QuizAnswerRequest,
  QuizQuestionRequest,
  QuizSessionSummaryResponse,
  QuizSetListResponse,
  QuizSetRequest
} from '../models/quiz-api.model';
import { QuizApiService } from '../services/quiz-api.service';

type ImageControlName = 'questionImageDataUrl' | 'answerImageDataUrl';

interface BackendErrorResponse {
  readonly message: string;
}

@Component({
  selector: 'app-admin-quiz-panel',
  imports: [ReactiveFormsModule, TuiBadge, TuiButton, TuiCard, TuiHeader, TuiInput, TuiTitle],
  templateUrl: './admin-quiz-panel.component.html',
  styleUrl: './admin-quiz-panel.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminQuizPanelComponent {
  private readonly quizApi = inject(QuizApiService);

  readonly quizSets = signal<readonly QuizSetListResponse[]>([]);
  readonly liveSessions = signal<readonly QuizSessionSummaryResponse[]>([]);
  readonly editingQuizSetId = signal<number | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly deleteTarget = signal<QuizSetListResponse | null>(null);

  readonly sortedLiveSessions = computed(() => [...this.liveSessions()].sort((a, b) => b.id - a.id));

  protected sessionPhaseAppearance(phase: string): string {
    switch (phase) {
      case 'QUESTION_OPEN':
        return 'positive';
      case 'QUESTION_LOCKED':
        return 'warning';
      case 'ANSWER_REVEALED':
        return 'info';
      case 'SCOREBOARD':
        return 'primary';
      default:
        return 'neutral';
    }
  }

  readonly quizForm = new FormGroup({
    title: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(140)]
    }),
    questions: new FormArray([this.createQuestionGroup()])
  });

  constructor() {
    void this.refresh();
  }

  protected get questions(): FormArray<FormGroup> {
    return this.quizForm.controls.questions;
  }

  protected answers(questionIndex: number): FormArray<FormGroup> {
    return this.questions.at(questionIndex).get('answers') as FormArray<FormGroup>;
  }

  protected addQuestion(): void {
    this.questions.push(this.createQuestionGroup());
  }

  protected removeQuestion(index: number): void {
    if (this.questions.length > 1) {
      this.questions.removeAt(index);
    }
  }

  protected addAnswer(questionIndex: number): void {
    const answers = this.answers(questionIndex);
    if (answers.length < 4) {
      answers.push(this.createAnswerGroup(false));
    }
  }

  protected removeAnswer(questionIndex: number, answerIndex: number): void {
    const answers = this.answers(questionIndex);
    if (answers.length > 2) {
      answers.removeAt(answerIndex);
      this.ensureOneCorrectAnswer(answers);
    }
  }

  protected markCorrect(questionIndex: number, answerIndex: number): void {
    this.answers(questionIndex).controls.forEach((answer, index) => answer.get('correct')?.setValue(index === answerIndex));
  }

  protected async setImage(event: Event, questionIndex: number, controlName: ImageControlName): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) {
      return;
    }
    if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type) || file.size > 500_000) {
      this.error.set('Image refusee: utilisez png, jpeg ou webp, 500 Ko maximum.');
      return;
    }
    const dataUrl = await this.readDataUrl(file);
    this.questions.at(questionIndex).get(controlName)?.setValue(dataUrl);
  }

  protected clearImage(questionIndex: number, controlName: ImageControlName): void {
    this.questions.at(questionIndex).get(controlName)?.setValue(null);
  }

  protected async refresh(): Promise<void> {
    await this.runAction(async () => {
      const [quizSets, liveSessions] = await Promise.all([this.quizApi.findQuizSets(), this.quizApi.findAdminLiveSessions()]);
      this.quizSets.set(quizSets);
      this.liveSessions.set(liveSessions);
    }, false);
  }

  protected async submitQuizSet(): Promise<void> {
    if (this.quizForm.invalid) {
      this.quizForm.markAllAsTouched();
      return;
    }

    const request = this.buildRequest();
    if (request === null) {
      this.error.set('Chaque question doit avoir entre 2 et 4 reponses et exactement une bonne reponse.');
      return;
    }

    await this.runAction(async () => {
      const id = this.editingQuizSetId();
      if (id === null) {
        await this.quizApi.createQuizSet(request);
        this.success.set('Ensemble de questions cree.');
      } else {
        await this.quizApi.updateQuizSet(id, request);
        this.success.set('Ensemble de questions mis a jour.');
      }
      this.resetForm();
      this.quizSets.set(await this.quizApi.findQuizSets());
    });
  }

  protected async editQuizSet(quizSet: QuizSetListResponse): Promise<void> {
    await this.runAction(async () => {
      const detail = await this.quizApi.findQuizSet(quizSet.id);
      this.editingQuizSetId.set(detail.id);
      this.quizForm.controls.title.setValue(detail.title);
      this.questions.clear();
      for (const question of detail.questions) {
        const group = this.createQuestionGroup();
        group.patchValue({
          text: question.text,
          durationSeconds: question.durationSeconds,
          questionImageDataUrl: question.questionImageDataUrl,
          answerImageDataUrl: question.answerImageDataUrl
        });
        const answers = group.get('answers') as FormArray<FormGroup>;
        answers.clear();
        for (const answer of question.answers) {
          answers.push(this.createAnswerGroup(answer.correct === true, answer.text));
        }
        this.questions.push(group);
      }
    });
  }

  protected resetForm(): void {
    this.editingQuizSetId.set(null);
    this.questions.clear();
    this.questions.push(this.createQuestionGroup());
    this.quizForm.reset({ title: '' });
  }

  protected requestDeleteQuizSet(quizSet: QuizSetListResponse): void {
    this.deleteTarget.set(quizSet);
  }

  protected cancelDelete(): void {
    this.deleteTarget.set(null);
  }

  protected async confirmDelete(): Promise<void> {
    const quizSet = this.deleteTarget();
    if (quizSet === null) {
      return;
    }

    this.deleteTarget.set(null);

    await this.runAction(async () => {
      await this.quizApi.deleteQuizSet(quizSet.id);
      this.quizSets.set(await this.quizApi.findQuizSets());
      this.liveSessions.set(await this.quizApi.findAdminLiveSessions());
      this.success.set('Ensemble de questions supprime.');
    });
  }

  protected async openSession(quizSetId: number): Promise<void> {
    await this.runAction(async () => {
      await this.quizApi.openSession(quizSetId);
      this.liveSessions.set(await this.quizApi.findAdminLiveSessions());
      this.success.set('Session ouverte: les joueurs peuvent rejoindre.');
    });
  }

  protected async transition(sessionId: number, action: 'start' | 'lock' | 'reveal' | 'scoreboard' | 'next'): Promise<void> {
    await this.runAction(async () => {
      if (action === 'start') await this.quizApi.startSession(sessionId);
      if (action === 'lock') await this.quizApi.lockQuestion(sessionId);
      if (action === 'reveal') await this.quizApi.revealAnswer(sessionId);
      if (action === 'scoreboard') await this.quizApi.showScoreboard(sessionId);
      if (action === 'next') await this.quizApi.nextQuestion(sessionId);
      this.liveSessions.set(await this.quizApi.findAdminLiveSessions());
    });
  }

  private createQuestionGroup(): FormGroup {
    return new FormGroup({
      text: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(500)] }),
      questionImageDataUrl: new FormControl<string | null>(null),
      answerImageDataUrl: new FormControl<string | null>(null),
      durationSeconds: new FormControl(30, { nonNullable: true, validators: [Validators.required, Validators.min(5), Validators.max(300)] }),
      answers: new FormArray([this.createAnswerGroup(true), this.createAnswerGroup(false)])
    });
  }

  private createAnswerGroup(correct: boolean, text = ''): FormGroup {
    return new FormGroup({
      text: new FormControl(text, { nonNullable: true, validators: [Validators.required, Validators.maxLength(240)] }),
      correct: new FormControl(correct, { nonNullable: true })
    });
  }

  private buildRequest(): QuizSetRequest | null {
    const questions = this.questions.controls.map((question): QuizQuestionRequest | null => {
      const answers = (question.get('answers') as FormArray<FormGroup>).controls.map((answer): QuizAnswerRequest => ({
        text: String(answer.get('text')?.value ?? '').trim(),
        correct: answer.get('correct')?.value === true
      }));
      if (answers.length < 2 || answers.length > 4 || answers.filter((answer) => answer.correct).length !== 1) {
        return null;
      }
      return {
        text: String(question.get('text')?.value ?? '').trim(),
        questionImageDataUrl: question.get('questionImageDataUrl')?.value as string | null,
        answerImageDataUrl: question.get('answerImageDataUrl')?.value as string | null,
        durationSeconds: Number(question.get('durationSeconds')?.value ?? 30),
        answers
      };
    });

    if (questions.some((question) => question === null)) {
      return null;
    }

    return {
      title: this.quizForm.controls.title.value.trim(),
      questions: questions as QuizQuestionRequest[]
    };
  }

  private ensureOneCorrectAnswer(answers: FormArray<FormGroup>): void {
    if (!answers.controls.some((answer) => answer.get('correct')?.value === true)) {
      answers.at(0).get('correct')?.setValue(true);
    }
  }

  private readDataUrl(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result));
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    });
  }

  private async runAction(action: () => Promise<void>, showSuccessReset = true): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    if (showSuccessReset) {
      this.success.set(null);
    }
    try {
      await action();
    } catch (error: unknown) {
      this.error.set(this.toErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private toErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error === 'object' && error.error !== null) {
      const backend = error.error as BackendErrorResponse;
      if (typeof backend.message === 'string') {
        return backend.message;
      }
    }
    return 'Operation quiz impossible pour le moment.';
  }
}
