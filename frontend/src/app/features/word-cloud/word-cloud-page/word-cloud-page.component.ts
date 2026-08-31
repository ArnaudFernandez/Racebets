import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TuiButton, TuiInput, TuiLoader } from '@taiga-ui/core';

import { WordCloudApiService } from '../services/word-cloud-api.service';
import { presentWord } from '../word-cloud-presentation';

@Component({
  selector: 'app-word-cloud-page',
  imports: [ReactiveFormsModule, TuiButton, TuiInput, TuiLoader],
  templateUrl: './word-cloud-page.component.html',
  styleUrl: './word-cloud-page.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WordCloudPageComponent {
  private readonly api = inject(WordCloudApiService);
  private readonly destroyRef = inject(DestroyRef);
  private synchronizedQuestionId: number | null = null;

  readonly snapshot = this.api.liveSnapshot;
  readonly unavailable = this.api.unavailable;
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly responseForm = new FormGroup({
    text: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(80)]
    })
  });
  readonly cloudWords = computed(() => {
    const words = this.snapshot()?.words ?? [];
    const maxCount = words.reduce((maximum, word) => Math.max(maximum, word.count), 1);
    return words.map((word) => presentWord(word, maxCount));
  });
  readonly liveAnnouncement = computed(() => {
    const snapshot = this.snapshot();
    if (snapshot === null) return 'En attente d’une question.';
    if (snapshot.status === 'OPEN') return `Nouvelle question ouverte : ${snapshot.text}`;
    if (snapshot.status === 'REVEALED') return `Le nuage de mots est maintenant affiché pour : ${snapshot.text}`;
    return 'La question est terminée.';
  });

  constructor() {
    this.api.startPlayerPolling(this.destroyRef);
    effect(() => {
      const snapshot = this.snapshot();
      if (snapshot?.status === 'OPEN' && snapshot.id !== this.synchronizedQuestionId) {
        this.synchronizedQuestionId = snapshot.id;
        this.responseForm.setValue({ text: snapshot.currentUserResponse ?? '' });
        this.responseForm.markAsPristine();
      }
      if (snapshot === null) this.synchronizedQuestionId = null;
    });
  }

  protected async submit(): Promise<void> {
    this.responseForm.markAllAsTouched();
    const snapshot = this.snapshot();
    if (snapshot?.status !== 'OPEN' || this.responseForm.invalid || this.submitting()) return;
    const text = this.responseForm.controls.text.value.trim();
    if (text.length === 0) return;

    this.submitting.set(true);
    this.error.set(null);
    try {
      await this.api.submitResponse(snapshot.id, text);
      this.responseForm.setValue({ text });
      this.responseForm.markAsPristine();
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.submitting.set(false);
    }
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 409) {
      return 'Les réponses viennent de fermer. Votre mot n’a pas été enregistré.';
    }
    return 'Votre réponse n’a pas pu être enregistrée. Réessayez dans un instant.';
  }
}
