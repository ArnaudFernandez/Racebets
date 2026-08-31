import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { TuiLoader } from '@taiga-ui/core';
import { EMPTY, catchError, exhaustMap, tap, timer } from 'rxjs';

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
  private readonly refreshError = signal<string | null>(null);

  readonly history = toSignal(
    timer(0, 1000).pipe(
      exhaustMap(() =>
        this.historyService.findAll().pipe(
          tap(() => this.refreshError.set(null)),
          catchError(() => {
            this.refreshError.set('Impossible d’actualiser votre historique pour le moment.');
            return EMPTY;
          })
        )
      )
    ),
    { initialValue: null }
  );
  readonly error = this.refreshError.asReadonly();
  readonly loading = computed(() => this.history() === null && this.error() === null);

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

}
