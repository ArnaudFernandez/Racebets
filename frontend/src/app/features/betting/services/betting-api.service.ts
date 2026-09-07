import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Subscription, catchError, exhaustMap, of, tap, timer } from 'rxjs';
import { firstValueFrom } from 'rxjs';

import { LiveRace } from '../models/betting.model';

@Injectable({ providedIn: 'root' })
export class BettingApiService {
  private readonly http = inject(HttpClient);
  private readonly pollingError = signal(false);
  private readonly liveRaceState = signal<LiveRace | null>(null);
  private pollingSubscription: Subscription | null = null;

  readonly unavailable = this.pollingError.asReadonly();
  readonly liveRace = this.liveRaceState.asReadonly();

  startWatching(): void {
    if (this.pollingSubscription !== null) return;
    this.pollingSubscription = timer(0, 1000).pipe(
      exhaustMap(() =>
        this.http.get<LiveRace | null>('/api/betting/live').pipe(
          tap((race) => {
            this.liveRaceState.set(race);
            this.pollingError.set(false);
          }),
          catchError(() => {
            this.pollingError.set(true);
            return of(null);
          })
        )
      )
    ).subscribe();
  }

  stopWatching(): void {
    this.pollingSubscription?.unsubscribe();
    this.pollingSubscription = null;
    this.liveRaceState.set(null);
    this.pollingError.set(false);
  }

  async placeBet(raceId: number, raceEntryId: number): Promise<LiveRace> {
    const response = await firstValueFrom(
      this.http.post<LiveRace>(`/api/betting/races/${raceId}/bets`, { raceEntryId })
    );
    this.liveRaceState.set(response);
    this.pollingError.set(false);
    return response;
  }
}
