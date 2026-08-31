import { HttpClient } from '@angular/common/http';
import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, exhaustMap, firstValueFrom, tap, timeout, timer } from 'rxjs';

import {
  WordCloudAdminSnapshot,
  WordCloudQuestionListItem,
  WordCloudQuestionRequest,
  WordCloudSnapshot
} from '../models/word-cloud.model';

@Injectable({ providedIn: 'root' })
export class WordCloudApiService {
  private readonly http = inject(HttpClient);
  private readonly playerSnapshot = signal<WordCloudSnapshot | null>(null);
  private readonly playerPollingUnavailable = signal(false);

  readonly liveSnapshot = this.playerSnapshot.asReadonly();
  readonly unavailable = this.playerPollingUnavailable.asReadonly();

  startPlayerPolling(destroyRef: DestroyRef): void {
    timer(0, 1000)
      .pipe(
        exhaustMap(() =>
          this.http.get<WordCloudSnapshot | null>('/api/word-cloud/live').pipe(
            timeout(2500),
            tap((snapshot) => {
              this.playerSnapshot.set(snapshot);
              this.playerPollingUnavailable.set(false);
            }),
            catchError(() => {
              this.playerPollingUnavailable.set(true);
              return EMPTY;
            })
          )
        ),
        takeUntilDestroyed(destroyRef)
      )
      .subscribe();
  }

  findQuestions(): Promise<readonly WordCloudQuestionListItem[]> {
    return firstValueFrom(this.http.get<readonly WordCloudQuestionListItem[]>('/api/admin/word-cloud/questions'));
  }

  createQuestion(request: WordCloudQuestionRequest): Promise<WordCloudQuestionListItem> {
    return firstValueFrom(this.http.post<WordCloudQuestionListItem>('/api/admin/word-cloud/questions', request));
  }

  updateQuestion(id: number, request: WordCloudQuestionRequest): Promise<WordCloudQuestionListItem> {
    return firstValueFrom(this.http.put<WordCloudQuestionListItem>(`/api/admin/word-cloud/questions/${id}`, request));
  }

  deleteQuestion(id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/admin/word-cloud/questions/${id}`));
  }

  findAdminQuestion(id: number): Promise<WordCloudAdminSnapshot> {
    return firstValueFrom(this.http.get<WordCloudAdminSnapshot>(`/api/admin/word-cloud/questions/${id}`));
  }

  openQuestion(id: number): Promise<WordCloudSnapshot> {
    return this.transition(id, 'open');
  }

  revealQuestion(id: number): Promise<WordCloudSnapshot> {
    return this.transition(id, 'reveal');
  }

  closeQuestion(id: number): Promise<WordCloudSnapshot> {
    return this.transition(id, 'close');
  }

  resetQuestion(id: number): Promise<WordCloudAdminSnapshot> {
    return firstValueFrom(
      this.http.post<WordCloudAdminSnapshot>(`/api/admin/word-cloud/questions/${id}/reset`, {})
    );
  }

  censorResponse(id: number, text: string): Promise<WordCloudAdminSnapshot> {
    return firstValueFrom(
      this.http.post<WordCloudAdminSnapshot>(`/api/admin/word-cloud/questions/${id}/responses/censor`, { text })
    );
  }

  findAdminLive(): Promise<WordCloudSnapshot | null> {
    return firstValueFrom(this.http.get<WordCloudSnapshot | null>('/api/admin/word-cloud/live'));
  }

  async submitResponse(questionId: number, text: string): Promise<WordCloudSnapshot> {
    const snapshot = await firstValueFrom(
      this.http.post<WordCloudSnapshot>(`/api/word-cloud/questions/${questionId}/responses`, { text })
    );
    this.playerSnapshot.set(snapshot);
    this.playerPollingUnavailable.set(false);
    return snapshot;
  }

  private transition(id: number, action: 'open' | 'reveal' | 'close'): Promise<WordCloudSnapshot> {
    return firstValueFrom(
      this.http.post<WordCloudSnapshot>(`/api/admin/word-cloud/questions/${id}/${action}`, {})
    );
  }
}
