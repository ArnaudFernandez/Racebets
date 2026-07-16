import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TuiButton, TuiLoader } from '@taiga-ui/core';

import { RaceHistorySummary } from '../models/admin-api.model';
import { AdminApiService } from '../services/admin-api.service';

@Component({
  selector: 'app-admin-race-history-panel',
  imports: [TuiButton, TuiLoader],
  templateUrl: './admin-race-history-panel.component.html',
  styleUrl: './admin-race-history-panel.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminRaceHistoryPanelComponent {
  private readonly adminApi = inject(AdminApiService);
  private readonly router = inject(Router);

  readonly races = signal<readonly RaceHistorySummary[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    void this.load();
  }

  protected open(raceId: number): void {
    void this.router.navigate(['/admin/history', raceId]);
  }

  protected async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.races.set(await this.adminApi.findRaceHistory());
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  protected formatDate(value: string): string {
    return new Intl.DateTimeFormat('fr-FR', {
      dateStyle: 'long', timeStyle: 'short'
    }).format(new Date(value));
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') return error.error.message;
    return 'Impossible de charger l’historique des courses.';
  }
}
