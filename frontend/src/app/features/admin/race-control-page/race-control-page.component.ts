import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TuiButton, TuiDialog, TuiLoader } from '@taiga-ui/core';
import { TuiBadge } from '@taiga-ui/kit';

import { HorseAdminResponse, RaceControl, RaceControlEntry, RaceState } from '../models/admin-api.model';
import { RaceOrderListComponent } from '../race-order-list/race-order-list.component';
import { AdminApiService } from '../services/admin-api.service';

const NEXT_STATE: Partial<Record<RaceState, RaceState>> = {
  CREATED: 'STANDBY',
  STANDBY: 'BET_STARTING',
  BET_STARTING: 'BETTING',
  BETTING: 'BET_CLOSED'
};
const RACE_REFRESH_ERROR = 'Impossible d’actualiser la course pour le moment.';

@Component({
  selector: 'app-race-control-page',
  imports: [RaceOrderListComponent, RouterLink, TuiBadge, TuiButton, TuiDialog, TuiLoader],
  templateUrl: './race-control-page.component.html',
  styleUrl: './race-control-page.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RaceControlPageComponent implements OnDestroy {
  private readonly adminApi = inject(AdminApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly raceId = Number(this.route.snapshot.paramMap.get('raceId'));
  private readonly pollId: number;
  private refreshing = false;
  private requestVersion = 0;

  readonly race = signal<RaceControl | null>(null);
  readonly horses = signal<readonly HorseAdminResponse[]>([]);
  readonly arrivalOrder = signal<readonly number[]>([]);
  readonly expectedResultOrder = signal<readonly number[]>([]);
  readonly editingResult = signal(false);
  readonly correctionDialogOpen = signal(false);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly nextState = computed(() => {
    const state = this.race()?.state;
    return state === undefined ? null : NEXT_STATE[state] ?? null;
  });
  readonly orderedEntries = computed(() => {
    const race = this.race();
    if (race === null) return [];
    const byId = new Map(race.entries.map((entry) => [entry.entryId, entry]));
    return this.arrivalOrder().map((id) => byId.get(id)).filter((entry): entry is RaceControlEntry => entry !== undefined);
  });
  readonly maxBetCount = computed(() => Math.max(1, ...this.race()?.entries.map((entry) => entry.betCount) ?? [1]));
  readonly resultChanged = computed(() => {
    const expected = this.expectedResultOrder();
    const current = this.arrivalOrder();
    return expected.length === current.length && expected.some((id, index) => current[index] !== id);
  });
  readonly originalWinnerName = computed(() => this.entryName(this.expectedResultOrder()[0]));
  readonly correctedWinnerName = computed(() => this.entryName(this.arrivalOrder()[0]));
  readonly winnerChanged = computed(() => this.originalWinnerName() !== this.correctedWinnerName());

  constructor() {
    void this.load();
    this.pollId = window.setInterval(() => {
      if (!this.loading() && !this.editingResult() && !this.correctionDialogOpen()) void this.refresh();
    }, 1500);
  }

  protected isSelected(horseId: number): boolean {
    return this.race()?.entries.some((entry) => entry.horseId === horseId) ?? false;
  }

  protected toggleRunner(horseId: number): void {
    const race = this.race();
    if (race?.state !== 'CREATED' || this.loading()) return;

    const selectedIds = new Set(race.entries.map((entry) => entry.horseId));
    if (selectedIds.has(horseId)) selectedIds.delete(horseId);
    else selectedIds.add(horseId);

    const orderedIds = this.horses()
      .filter((horse) => selectedIds.has(horse.id))
      .map((horse) => horse.id);
    void this.run(async () => {
      this.applyRace(await this.adminApi.selectRaceRunners(this.raceId, orderedIds), true);
      this.success.set(
        orderedIds.length === 0
          ? 'Aucun cheval sélectionné.'
          : orderedIds.length === 1
            ? '1 cheval sélectionné.'
            : `${orderedIds.length} chevaux sélectionnés.`
      );
    });
  }

  protected async clearLive(): Promise<void> {
    await this.run(async () => {
      this.applyRace(await this.adminApi.clearRaceFromLive(this.raceId), false);
      this.success.set('Le résultat a été retiré de l’écran Paris Live.');
    });
  }

  ngOnDestroy(): void {
    window.clearInterval(this.pollId);
  }

  protected async advance(): Promise<void> {
    const target = this.nextState();
    if (target === null) return;
    await this.run(async () => {
      this.applyRace(await this.adminApi.transitionRace(this.raceId, target), false);
      this.success.set(this.transitionSuccess(target));
    });
  }

  protected reorderEntries(order: readonly number[]): void {
    const race = this.race();
    const editable = race?.state === 'BET_CLOSED' || (race?.state === 'FINISHED' && this.editingResult());
    if (!editable || this.loading()) return;
    if (order.length !== race.entries.length || new Set(order).size !== order.length) return;
    const entryIds = new Set(race.entries.map((entry) => entry.entryId));
    if (!order.every((entryId) => entryIds.has(entryId))) return;
    this.arrivalOrder.set([...order]);
  }

  protected async publishResult(): Promise<void> {
    if (this.race()?.state !== 'BET_CLOSED' || this.arrivalOrder().length === 0) return;
    await this.run(async () => {
      this.applyRace(await this.adminApi.publishRaceResult(this.raceId, this.arrivalOrder()), true);
      this.success.set('Résultat envoyé à tous les participants.');
    });
  }

  protected editResult(): void {
    const race = this.race();
    if (race?.state !== 'FINISHED' || this.loading()) return;
    const order = this.officialOrder(race);
    this.expectedResultOrder.set(order);
    this.arrivalOrder.set(order);
    this.editingResult.set(true);
    this.error.set(null);
    this.success.set(null);
  }

  protected cancelResultEdition(): void {
    this.correctionDialogOpen.set(false);
    this.editingResult.set(false);
    this.arrivalOrder.set(this.expectedResultOrder());
    this.expectedResultOrder.set([]);
  }

  protected requestResultCorrection(): void {
    if (!this.resultChanged() || this.loading()) return;
    this.correctionDialogOpen.set(true);
  }

  protected cancelResultCorrection(): void {
    this.correctionDialogOpen.set(false);
  }

  protected async confirmResultCorrection(): Promise<void> {
    if (!this.resultChanged() || this.loading()) return;
    this.correctionDialogOpen.set(false);
    this.requestVersion++;
    this.loading.set(true);
    this.error.set(null);
    this.success.set(null);
    try {
      const history = await this.adminApi.correctRaceResult(
        this.raceId,
        this.expectedResultOrder(),
        this.arrivalOrder()
      );
      const ranks = new Map(history.result.map((entry) => [entry.entryId, entry.rank]));
      const race = this.race();
      if (race !== null) {
        this.applyRace({
          ...race,
          entries: race.entries.map((entry) => ({
            ...entry,
            rank: ranks.get(entry.entryId) ?? entry.rank
          }))
        }, true);
      }
      this.editingResult.set(false);
      this.expectedResultOrder.set([]);
      this.success.set('Le classement et les résultats des joueurs ont été corrigés.');
    } catch (error: unknown) {
      await this.handleCorrectionError(error);
    } finally {
      this.loading.set(false);
    }
  }

  protected stateLabel(state: RaceState): string {
    return ({ CREATED: 'Brouillon', STANDBY: 'À l’affiche', BET_STARTING: 'Ouverture imminente', BETTING: 'Paris ouverts', BET_CLOSED: 'Paris clos', FINISHED: 'Terminée' })[state];
  }

  protected nextActionLabel(state: RaceState): string {
    return ({ CREATED: 'Afficher la course', STANDBY: 'Annoncer l’ouverture', BET_STARTING: 'Ouvrir les paris', BETTING: 'Arrêter les paris', BET_CLOSED: '', FINISHED: '' })[state];
  }

  protected nextActionHint(state: RaceState): string {
    return ({
      CREATED: 'Les partants seront visibles par tous les participants.',
      STANDBY: 'L’écran indiquera que les votes vont bientôt commencer.',
      BET_STARTING: 'Les participants pourront enregistrer leur choix.',
      BETTING: 'Les choix seront verrouillés et ne pourront plus être modifiés.',
      BET_CLOSED: '',
      FINISHED: ''
    })[state];
  }

  protected percent(entry: RaceControlEntry): number {
    return entry.betCount / this.maxBetCount() * 100;
  }

  private async refresh(): Promise<void> {
    if (this.refreshing) return;
    this.refreshing = true;
    const requestVersion = this.requestVersion;
    try {
      const race = await this.adminApi.getRaceControl(this.raceId);
      if (requestVersion !== this.requestVersion || this.editingResult() || this.correctionDialogOpen()) return;
      this.applyRace(race, race.state === 'FINISHED');
      if (this.error() === RACE_REFRESH_ERROR) this.error.set(null);
    } catch {
      if (requestVersion !== this.requestVersion) return;
      this.error.set(RACE_REFRESH_ERROR);
    } finally {
      this.refreshing = false;
    }
  }

  private async load(): Promise<void> {
    this.refreshing = true;
    try {
      const [race, horses] = await Promise.all([
        this.adminApi.getRaceControl(this.raceId),
        this.adminApi.findHorses()
      ]);
      this.horses.set(horses);
      if (this.editingResult() || this.correctionDialogOpen()) return;
      this.applyRace(race, true);
      this.error.set(null);
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.refreshing = false;
    }
  }

  private applyRace(race: RaceControl, resetOrder: boolean): void {
    this.race.set(race);
    const currentIds = this.arrivalOrder();
    const receivedIds = race.entries.map((entry) => entry.entryId);
    const sameEntries = currentIds.length === receivedIds.length && currentIds.every((id) => receivedIds.includes(id));
    if (resetOrder || !sameEntries) {
      this.arrivalOrder.set([...race.entries]
        .sort((left, right) => (left.rank ?? left.horseNumber) - (right.rank ?? right.horseNumber))
        .map((entry) => entry.entryId));
    }
  }

  private officialOrder(race: RaceControl): readonly number[] {
    return [...race.entries]
      .sort((left, right) => (left.rank ?? left.horseNumber) - (right.rank ?? right.horseNumber))
      .map((entry) => entry.entryId);
  }

  private entryName(entryId: number | undefined): string {
    return this.race()?.entries.find((entry) => entry.entryId === entryId)?.horseName ?? '';
  }

  private async handleCorrectionError(error: unknown): Promise<void> {
    if (error instanceof HttpErrorResponse && error.status === 409) {
      try {
        this.applyRace(await this.adminApi.getRaceControl(this.raceId), true);
        this.editingResult.set(false);
        this.expectedResultOrder.set([]);
        this.error.set(
          'Le résultat a été modifié par un autre administrateur. Le classement actuel a été rechargé.'
        );
        return;
      } catch {
        // Preserve the original conflict when the latest race cannot be loaded.
      }
    }
    this.error.set(this.errorMessage(error));
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.requestVersion++;
    this.loading.set(true);
    this.error.set(null);
    this.success.set(null);
    try {
      await action();
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private transitionSuccess(state: RaceState): string {
    return ({ STANDBY: 'La course est visible par les participants.', BET_STARTING: 'Les participants sont prévenus.', BETTING: 'Les paris sont ouverts.', BET_CLOSED: 'Les paris sont verrouillés.', CREATED: '', FINISHED: '' })[state];
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') return error.error.message;
    return 'Impossible de mettre à jour la course pour le moment.';
  }
}
