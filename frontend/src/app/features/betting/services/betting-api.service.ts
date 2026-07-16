import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, exhaustMap, of, tap, timer } from 'rxjs';
import { firstValueFrom } from 'rxjs';

import { LiveRace } from '../models/betting.model';

@Injectable({ providedIn: 'root' })
export class BettingApiService {
  private readonly http = inject(HttpClient);
  private readonly pollingError = signal(false);

  readonly unavailable = this.pollingError.asReadonly();
  readonly liveRace = toSignal(
    timer(0, 1000).pipe(
      exhaustMap(() =>
        this.http.get<LiveRace | null>('/api/betting/live').pipe(
          tap(() => this.pollingError.set(false)),
          catchError(() => {
            this.pollingError.set(true);
            return of(null);
          })
        )
      )
    ),
    { initialValue: null }
  );

  async placeBet(raceId: number, raceEntryId: number): Promise<LiveRace> {
    const response = await firstValueFrom(
      this.http.post<LiveRace>(`/api/betting/races/${raceId}/bets`, { raceEntryId })
    );
    this.pollingError.set(false);
    return response;
  }
}
