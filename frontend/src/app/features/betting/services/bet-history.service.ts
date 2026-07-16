import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { BetHistoryAvailability, BetHistoryEntry } from '../models/bet-history.model';

@Injectable({ providedIn: 'root' })
export class BetHistoryService {
  private readonly http = inject(HttpClient);
  private readonly availability = signal(false);
  private pollId: number | null = null;

  readonly hasHistory = this.availability.asReadonly();

  async findAll(): Promise<readonly BetHistoryEntry[]> {
    const history = await firstValueFrom(this.http.get<readonly BetHistoryEntry[]>('/api/betting/history'));
    this.availability.set(history.length > 0);
    return history;
  }

  async refreshAvailability(): Promise<void> {
    try {
      const response = await firstValueFrom(
        this.http.get<BetHistoryAvailability>('/api/betting/history/availability')
      );
      this.availability.set(response.hasHistory);
    } catch {
      this.availability.set(false);
    }
  }

  startWatching(): void {
    if (this.pollId !== null) return;
    void this.refreshAvailability();
    this.pollId = window.setInterval(() => void this.refreshAvailability(), 2000);
  }

  stopWatching(): void {
    if (this.pollId === null) return;
    window.clearInterval(this.pollId);
    this.pollId = null;
  }

  clearAvailability(): void {
    this.stopWatching();
    this.availability.set(false);
  }
}
