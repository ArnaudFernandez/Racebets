import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, computed, inject, signal } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TuiButton, TuiInput, TuiLoader } from '@taiga-ui/core';

import { QuizAnswerRequest, QuizQuestionRequest, QuizSetRequest } from '../models/quiz-api.model';
import { QuizApiService } from '../services/quiz-api.service';

type ImageControlName = 'questionImageDataUrl' | 'answerImageDataUrl';
const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

interface BackendErrorResponse {
  readonly message: string;
}

@Component({
  selector: 'app-quiz-editor-page',
  imports: [ReactiveFormsModule, RouterLink, TuiButton, TuiInput, TuiLoader],
  templateUrl: './quiz-editor-page.component.html',
  styleUrl: './quiz-editor-page.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuizEditorPageComponent {
  private readonly quizApi = inject(QuizApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly quizId = this.parseQuizId();

  readonly editing = this.quizId !== null;
  readonly selectedQuestionIndex = signal(0);
  readonly loading = signal(this.editing);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly completedQuestions = computed(() => this.questions.controls.filter((question) => question.valid).length);

  readonly quizForm = new FormGroup({
    title: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(140)] }),
    questions: new FormArray([this.createQuestionGroup()])
  });

  constructor() {
    if (this.quizId !== null) void this.load(this.quizId);
  }

  protected get questions(): FormArray<FormGroup> {
    return this.quizForm.controls.questions;
  }

  protected selectedQuestion(): FormGroup {
    return this.questions.at(this.selectedQuestionIndex());
  }

  protected answers(questionIndex = this.selectedQuestionIndex()): FormArray<FormGroup> {
    return this.questions.at(questionIndex).get('answers') as FormArray<FormGroup>;
  }

  protected selectQuestion(index: number): void {
    this.selectedQuestionIndex.set(index);
  }

  protected addQuestion(): void {
    this.questions.push(this.createQuestionGroup());
    this.selectedQuestionIndex.set(this.questions.length - 1);
  }

  protected duplicateQuestion(index: number): void {
    const source = this.questions.at(index).getRawValue();
    const copy = this.createQuestionGroup();
    copy.patchValue({
      text: source.text,
      durationSeconds: source.durationSeconds,
      questionImageDataUrl: source.questionImageDataUrl,
      answerImageDataUrl: source.answerImageDataUrl
    });
    const copiedAnswers = copy.get('answers') as FormArray<FormGroup>;
    copiedAnswers.clear();
    for (const answer of source.answers) copiedAnswers.push(this.createAnswerGroup(answer.correct, answer.text));
    this.questions.insert(index + 1, copy);
    this.selectedQuestionIndex.set(index + 1);
  }

  protected removeQuestion(index: number): void {
    if (this.questions.length === 1) return;
    this.questions.removeAt(index);
    this.selectedQuestionIndex.set(Math.min(index, this.questions.length - 1));
  }

  protected moveQuestion(index: number, direction: -1 | 1): void {
    const destination = index + direction;
    if (destination < 0 || destination >= this.questions.length) return;
    const question = this.questions.at(index);
    this.questions.removeAt(index);
    this.questions.insert(destination, question);
    this.selectedQuestionIndex.set(destination);
  }

  protected addAnswer(): void {
    const answers = this.answers();
    if (answers.length < 4) answers.push(this.createAnswerGroup(false));
  }

  protected removeAnswer(answerIndex: number): void {
    const answers = this.answers();
    if (answers.length <= 2) return;
    answers.removeAt(answerIndex);
    if (!answers.controls.some((answer) => answer.get('correct')?.value === true)) {
      answers.at(0).get('correct')?.setValue(true);
    }
  }

  protected markCorrect(answerIndex: number): void {
    this.answers().controls.forEach((answer, index) => answer.get('correct')?.setValue(index === answerIndex));
  }

  protected async setImage(event: Event, controlName: ImageControlName): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
      this.error.set('Format non pris en charge. Utilisez une image PNG, JPEG ou WebP.');
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      this.error.set('Cette image dépasse 5 Mo. Choisissez un fichier plus léger.');
      return;
    }
    const question = this.selectedQuestion();
    const dataUrl = await this.readDataUrl(file);
    question.get(controlName)?.setValue(dataUrl);
    this.error.set(null);
    // FileReader completes after the originating change event. With OnPush,
    // explicitly schedule a render so the first selected image is displayed.
    this.changeDetectorRef.markForCheck();
  }

  protected clearImage(controlName: ImageControlName): void {
    this.selectedQuestion().get(controlName)?.setValue(null);
    this.changeDetectorRef.markForCheck();
  }

  protected async save(): Promise<void> {
    if (this.quizForm.invalid) {
      this.quizForm.markAllAsTouched();
      this.error.set('Complétez le titre, chaque question et toutes les réponses avant d’enregistrer.');
      return;
    }
    const request = this.buildRequest();
    if (request === null) {
      this.error.set('Chaque question doit contenir une seule bonne réponse.');
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    try {
      if (this.quizId === null) await this.quizApi.createQuizSet(request);
      else await this.quizApi.updateQuizSet(this.quizId, request);
      await this.router.navigate(['/admin'], { queryParams: { section: 'quizzes' } });
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }

  private async load(id: number): Promise<void> {
    try {
      const quiz = await this.quizApi.findQuizSet(id);
      this.quizForm.controls.title.setValue(quiz.title);
      this.questions.clear();
      for (const question of quiz.questions) {
        const group = this.createQuestionGroup();
        group.patchValue({
          text: question.text,
          durationSeconds: question.durationSeconds,
          questionImageDataUrl: question.questionImageDataUrl,
          answerImageDataUrl: question.answerImageDataUrl
        });
        const answers = group.get('answers') as FormArray<FormGroup>;
        answers.clear();
        for (const answer of question.answers) answers.push(this.createAnswerGroup(answer.correct === true, answer.text));
        this.questions.push(group);
      }
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
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
      if (answers.filter((answer) => answer.correct).length !== 1) return null;
      return {
        text: String(question.get('text')?.value ?? '').trim(),
        durationSeconds: Number(question.get('durationSeconds')?.value ?? 30),
        questionImageDataUrl: question.get('questionImageDataUrl')?.value as string | null,
        answerImageDataUrl: question.get('answerImageDataUrl')?.value as string | null,
        answers
      };
    });
    if (questions.some((question) => question === null)) return null;
    return { title: this.quizForm.controls.title.value.trim(), questions: questions as QuizQuestionRequest[] };
  }

  private readDataUrl(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result));
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    });
  }

  private parseQuizId(): number | null {
    const value = this.route.snapshot.paramMap.get('quizSetId');
    if (value === null) return null;
    const id = Number(value);
    return Number.isFinite(id) ? id : null;
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error === 'object' && error.error !== null) {
      const backend = error.error as BackendErrorResponse;
      if (typeof backend.message === 'string') return backend.message;
    }
    return 'Impossible d’enregistrer ce questionnaire pour le moment.';
  }
}
