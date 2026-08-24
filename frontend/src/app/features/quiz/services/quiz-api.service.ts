import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import {
  QuizSessionSnapshotResponse,
  QuizSessionSummaryResponse,
  QuizSetDetailResponse,
  QuizSetListResponse,
  QuizSetRequest
} from '../models/quiz-api.model';

@Injectable({ providedIn: 'root' })
export class QuizApiService {
  private readonly http = inject(HttpClient);

  findQuizSets(): Promise<readonly QuizSetListResponse[]> {
    return firstValueFrom(this.http.get<readonly QuizSetListResponse[]>('/api/admin/quizzes'));
  }

  findQuizSet(id: number): Promise<QuizSetDetailResponse> {
    return firstValueFrom(this.http.get<QuizSetDetailResponse>(`/api/admin/quizzes/${id}`));
  }

  createQuizSet(request: QuizSetRequest): Promise<QuizSetDetailResponse> {
    return firstValueFrom(this.http.post<QuizSetDetailResponse>('/api/admin/quizzes', request));
  }

  updateQuizSet(id: number, request: QuizSetRequest): Promise<QuizSetDetailResponse> {
    return firstValueFrom(this.http.put<QuizSetDetailResponse>(`/api/admin/quizzes/${id}`, request));
  }

  deleteQuizSet(id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/admin/quizzes/${id}`));
  }

  openSession(quizSetId: number): Promise<QuizSessionSnapshotResponse> {
    return firstValueFrom(this.http.post<QuizSessionSnapshotResponse>(`/api/admin/quizzes/${quizSetId}/open`, {}));
  }

  findAdminLiveSessions(): Promise<readonly QuizSessionSummaryResponse[]> {
    return firstValueFrom(this.http.get<readonly QuizSessionSummaryResponse[]>('/api/admin/quizzes/sessions/live'));
  }

  findAdminSession(sessionId: number): Promise<QuizSessionSnapshotResponse> {
    return firstValueFrom(this.http.get<QuizSessionSnapshotResponse>(`/api/admin/quizzes/sessions/${sessionId}`));
  }

  startSession(sessionId: number): Promise<QuizSessionSnapshotResponse> {
    return this.transition(sessionId, 'start');
  }

  lockQuestion(sessionId: number): Promise<QuizSessionSnapshotResponse> {
    return this.transition(sessionId, 'lock');
  }

  revealAnswer(sessionId: number): Promise<QuizSessionSnapshotResponse> {
    return this.transition(sessionId, 'reveal');
  }

  showScoreboard(sessionId: number): Promise<QuizSessionSnapshotResponse> {
    return this.transition(sessionId, 'scoreboard');
  }

  nextQuestion(sessionId: number): Promise<QuizSessionSnapshotResponse> {
    return this.transition(sessionId, 'next');
  }

  findLiveSessions(): Promise<readonly QuizSessionSummaryResponse[]> {
    return firstValueFrom(this.http.get<readonly QuizSessionSummaryResponse[]>('/api/quizzes/live'));
  }

  findSession(sessionId: number): Promise<QuizSessionSnapshotResponse> {
    return firstValueFrom(this.http.get<QuizSessionSnapshotResponse>(`/api/quizzes/sessions/${sessionId}`));
  }

  joinSession(sessionId: number): Promise<QuizSessionSnapshotResponse> {
    return firstValueFrom(this.http.post<QuizSessionSnapshotResponse>(`/api/quizzes/sessions/${sessionId}/join`, {}));
  }

  answer(sessionId: number, answerId: number): Promise<QuizSessionSnapshotResponse> {
    return firstValueFrom(this.http.post<QuizSessionSnapshotResponse>(`/api/quizzes/sessions/${sessionId}/answers`, { answerId }));
  }

  private transition(sessionId: number, action: string): Promise<QuizSessionSnapshotResponse> {
    return firstValueFrom(this.http.post<QuizSessionSnapshotResponse>(`/api/admin/quizzes/sessions/${sessionId}/${action}`, {}));
  }
}
