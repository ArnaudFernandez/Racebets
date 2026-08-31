import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { EMPTY, Observable, Subscription, catchError, exhaustMap, tap, timer } from 'rxjs';

import { BetHistoryAvailability, BetHistoryEntry } from '../models/bet-history.model';

@Injectable({ providedIn: 'root' })
export class BetHistoryService {
  private readonly http = inject(HttpClient);
  private readonly availability = signal(false);
  private watcher: Subscription | null = null;

  readonly hasHistory = this.availability.asReadonly();

  findAll(): Observable<readonly BetHistoryEntry[]> {
    return this.http.get<readonly BetHistoryEntry[]>('/api/betting/history').pipe(
      tap((history) => this.availability.set(history.length > 0))
    );
  }

  startWatching(): void {
    if (this.watcher !== null) return;
    this.watcher = timer(0, 2000)
      .pipe(
        exhaustMap(() =>
          this.http.get<BetHistoryAvailability>('/api/betting/history/availability').pipe(
            catchError(() => EMPTY)
          )
        )
      )
      .subscribe((response) => this.availability.set(response.hasHistory));
  }

  stopWatching(): void {
    this.watcher?.unsubscribe();
    this.watcher = null;
  }

  clearAvailability(): void {
    this.stopWatching();
    this.availability.set(false);
  }
}
