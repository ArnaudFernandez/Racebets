import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TuiButton, TuiLoader } from '@taiga-ui/core';

import { RaceHistoryDetail } from '../models/admin-api.model';
import { AdminApiService } from '../services/admin-api.service';

@Component({
  selector: 'app-race-history-detail-page',
  imports: [RouterLink, TuiButton, TuiLoader],
  templateUrl: './race-history-detail-page.component.html',
  styleUrl: './race-history-detail-page.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RaceHistoryDetailPageComponent {
  private readonly adminApi = inject(AdminApiService);
  private readonly raceId = Number(inject(ActivatedRoute).snapshot.paramMap.get('raceId'));

  readonly history = signal<RaceHistoryDetail | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor() {
    void this.load();
  }

  protected formatDate(value: string): string {
    return new Intl.DateTimeFormat('fr-FR', {
      dateStyle: 'long', timeStyle: 'medium'
    }).format(new Date(value));
  }

  protected formatTime(value: string): string {
    return new Intl.DateTimeFormat('fr-FR', {
      hour: '2-digit', minute: '2-digit', second: '2-digit', fractionalSecondDigits: 3
    }).format(new Date(value));
  }

  private async load(): Promise<void> {
    try {
      this.history.set(await this.adminApi.getRaceHistoryDetail(this.raceId));
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') return error.error.message;
    return 'Impossible de charger le détail de cette course.';
  }
}
