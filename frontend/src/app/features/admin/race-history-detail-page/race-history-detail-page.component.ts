import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TuiButton, TuiDialog, TuiLoader } from '@taiga-ui/core';

import { RaceHistoryDetail, RaceHistoryResult } from '../models/admin-api.model';
import { RaceOrderListComponent } from '../race-order-list/race-order-list.component';
import { AdminApiService } from '../services/admin-api.service';

@Component({
  selector: 'app-race-history-detail-page',
  imports: [RaceOrderListComponent, RouterLink, TuiButton, TuiDialog, TuiLoader],
  templateUrl: './race-history-detail-page.component.html',
  styleUrl: './race-history-detail-page.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RaceHistoryDetailPageComponent {
  private readonly adminApi = inject(AdminApiService);
  private readonly raceId = Number(inject(ActivatedRoute).snapshot.paramMap.get('raceId'));

  readonly history = signal<RaceHistoryDetail | null>(null);
  readonly arrivalOrder = signal<readonly number[]>([]);
  readonly editingResult = signal(false);
  readonly correctionDialogOpen = signal(false);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly orderedResult = computed(() => {
    const history = this.history();
    if (history === null) return [];
    const byId = new Map(history.result.map((entry) => [entry.entryId, entry]));
    return this.arrivalOrder()
      .map((id) => byId.get(id))
      .filter((entry): entry is RaceHistoryResult => entry !== undefined);
  });
  readonly resultChanged = computed(() => {
    const history = this.history();
    if (history === null) return false;
    const originalOrder = [...history.result]
      .sort((left, right) => left.rank - right.rank)
      .map((entry) => entry.entryId);
    const currentOrder = this.arrivalOrder();
    return originalOrder.some((id, index) => currentOrder[index] !== id);
  });
  readonly originalWinnerName = computed(
    () => this.history()?.result.find((entry) => entry.rank === 1)?.horseName ?? ''
  );
  readonly correctedWinnerName = computed(() => this.orderedResult()[0]?.horseName ?? '');
  readonly winnerChanged = computed(() => this.originalWinnerName() !== this.correctedWinnerName());

  constructor() {
    void this.load();
  }

  protected formatDate(value: string): string {
    return new Intl.DateTimeFormat('fr-FR', {
      dateStyle: 'long',
      timeStyle: 'medium'
    }).format(new Date(value));
  }

  protected formatTime(value: string): string {
    return new Intl.DateTimeFormat('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      fractionalSecondDigits: 3
    }).format(new Date(value));
  }

  protected editResult(): void {
    this.resetOrder();
    this.editingResult.set(true);
    this.error.set(null);
    this.success.set(null);
  }

  protected cancelResultEdition(): void {
    this.correctionDialogOpen.set(false);
    this.editingResult.set(false);
    this.resetOrder();
  }

  protected reorderEntries(order: readonly number[]): void {
    if (!this.editingResult() || this.saving()) return;
    const entries = this.history()?.result ?? [];
    if (order.length !== entries.length || new Set(order).size !== order.length) return;
    const entryIds = new Set(entries.map((entry) => entry.entryId));
    if (!order.every((entryId) => entryIds.has(entryId))) return;
    this.arrivalOrder.set([...order]);
  }

  protected requestResultCorrection(): void {
    if (!this.resultChanged() || this.saving()) return;
    this.correctionDialogOpen.set(true);
  }

  protected cancelResultCorrection(): void {
    this.correctionDialogOpen.set(false);
  }

  protected async confirmResultCorrection(): Promise<void> {
    if (!this.resultChanged() || this.saving()) return;
    this.correctionDialogOpen.set(false);
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    try {
      this.applyHistory(
        await this.adminApi.correctRaceResult(
          this.raceId,
          this.officialOrder(),
          this.arrivalOrder()
        )
      );
      this.editingResult.set(false);
      this.success.set('Le classement et les résultats des votes ont été corrigés.');
    } catch (error: unknown) {
      await this.handleCorrectionError(error);
    } finally {
      this.saving.set(false);
    }
  }

  private async load(): Promise<void> {
    try {
      this.applyHistory(await this.adminApi.getRaceHistoryDetail(this.raceId));
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error, 'Impossible de charger le détail de cette course.'));
    } finally {
      this.loading.set(false);
    }
  }

  private applyHistory(history: RaceHistoryDetail): void {
    this.history.set(history);
    this.resetOrder();
  }

  private resetOrder(): void {
    this.arrivalOrder.set(this.officialOrder());
  }

  private officialOrder(): readonly number[] {
    return [...(this.history()?.result ?? [])]
      .sort((left, right) => left.rank - right.rank)
      .map((entry) => entry.entryId);
  }

  private async handleCorrectionError(error: unknown): Promise<void> {
    if (error instanceof HttpErrorResponse && error.status === 409) {
      try {
        this.applyHistory(await this.adminApi.getRaceHistoryDetail(this.raceId));
        this.editingResult.set(false);
        this.error.set(
          'Le résultat a été modifié par un autre administrateur. Le classement actuel a été rechargé.'
        );
        return;
      } catch {
        // Preserve the original conflict when the latest history cannot be loaded.
      }
    }

    this.error.set(this.errorMessage(error, 'Impossible de corriger le résultat de cette course.'));
  }

  private errorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string')
      return error.error.message;
    return fallback;
  }
}
