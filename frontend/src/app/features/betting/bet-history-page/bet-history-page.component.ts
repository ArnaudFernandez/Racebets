import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { TuiLoader } from '@taiga-ui/core';

import { BetHistoryEntry } from '../models/bet-history.model';
import { BetHistoryService } from '../services/bet-history.service';

@Component({
  selector: 'app-bet-history-page',
  imports: [TuiLoader],
  templateUrl: './bet-history-page.component.html',
  styleUrl: './bet-history-page.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BetHistoryPageComponent {
  private readonly historyService = inject(BetHistoryService);

  readonly history = signal<readonly BetHistoryEntry[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor() {
    void this.load();
  }

  protected formatDate(value: string): string {
    return new Intl.DateTimeFormat('fr-FR', {
      dateStyle: 'long',
      timeStyle: 'short'
    }).format(new Date(value));
  }

  protected formatTime(value: string): string {
    return new Intl.DateTimeFormat('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    }).format(new Date(value));
  }

  private async load(): Promise<void> {
    try {
      this.history.set(await this.historyService.findAll());
    } catch {
      this.error.set('Impossible de charger votre historique pour le moment.');
    } finally {
      this.loading.set(false);
    }
  }
}
