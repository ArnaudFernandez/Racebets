import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TuiButton, TuiInput, TuiLoader, TuiTitle } from '@taiga-ui/core';
import { TuiCard, TuiHeader } from '@taiga-ui/layout';
import { TuiBadge, TuiTabs } from '@taiga-ui/kit';

import { AdminUserPanelComponent } from '../admin-user-panel/admin-user-panel.component';
import { AppFeaturePanelComponent } from '../app-feature-panel/app-feature-panel.component';
import { AdminQuizPanelComponent } from '../../quiz/admin-quiz-panel/admin-quiz-panel.component';
import {
  HorseAdminRequest,
  HorseAdminResponse,
  RaceAdminRequest,
  RaceAdminResponse,
  RaceEntryAdminRequest,
  RaceEntryAdminResponse,
  RaceState
} from '../models/admin-api.model';
import { AdminApiService } from '../services/admin-api.service';

type AdminSection = 'features' | 'horses' | 'races' | 'entries' | 'users' | 'quizzes';
type DeleteTarget =
  | { readonly type: 'horse'; readonly id: number; readonly label: string }
  | { readonly type: 'race'; readonly id: number; readonly label: string }
  | { readonly type: 'raceEntry'; readonly id: number; readonly label: string };

interface BackendErrorResponse {
  readonly message: string;
}

@Component({
  selector: 'app-admin-dashboard',
  imports: [AdminQuizPanelComponent, AdminUserPanelComponent, AppFeaturePanelComponent, ReactiveFormsModule, TuiBadge, TuiButton, TuiCard, TuiHeader, TuiInput, TuiLoader, TuiTabs, TuiTitle],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminDashboardComponent {
  private readonly adminApi = inject(AdminApiService);

  readonly sections: readonly { readonly id: AdminSection; readonly label: string }[] = [
    { id: 'features', label: 'Affichage' },
    { id: 'horses', label: 'Chevaux' },
    { id: 'races', label: 'Courses' },
    { id: 'entries', label: 'Participations' },
    { id: 'users', label: 'Utilisateurs' },
    { id: 'quizzes', label: 'Quiz' }
  ];

  readonly activeSection = signal<AdminSection>('features');
  readonly activeSectionIndex = computed(() => this.sections.findIndex((section) => section.id === this.activeSection()));
  readonly horses = signal<readonly HorseAdminResponse[]>([]);
  readonly races = signal<readonly RaceAdminResponse[]>([]);
  readonly raceEntries = signal<readonly RaceEntryAdminResponse[]>([]);
  readonly loading = signal(false);
  readonly loadingLabel = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly deleteTarget = signal<DeleteTarget | null>(null);
  readonly editingHorseId = signal<number | null>(null);
  readonly editingRaceId = signal<number | null>(null);
  readonly editingRaceEntryId = signal<number | null>(null);

  readonly selectedRaceId = signal<number | null>(null);
  readonly sortedRaceEntries = computed(() =>
    [...this.raceEntries()].sort((left, right) => left.raceName.localeCompare(right.raceName) || left.horseNumber - right.horseNumber)
  );

  readonly raceStates: readonly RaceState[] = ['CREATED', 'STANDBY', 'BET_STARTING', 'BETTING', 'BET_CLOSED', 'FINISHED'];

  readonly horseForm = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100)]
    })
  });

  readonly raceForm = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(120)]
    }),
    raceImgUrl: new FormControl<string | null>(null, Validators.maxLength(500)),
    state: new FormControl<RaceState | null>('CREATED')
  });

  readonly raceEntryForm = new FormGroup({
    raceId: new FormControl<number | null>(null, Validators.required),
    horseId: new FormControl<number | null>(null, Validators.required),
    horseNumber: new FormControl<number | null>(null, [Validators.required, Validators.min(1)]),
    rank: new FormControl<number | null>(null, Validators.min(1))
  });

  constructor() {
    void this.refreshAll();
  }

  protected sectionCount(section: AdminSection): number | null {
    switch (section) {
      case 'horses':
        return this.horses().length;
      case 'races':
        return this.races().length;
      case 'entries':
        return this.raceEntries().length;
      default:
        return null;
    }
  }

  protected raceStateAppearance(state: RaceState): string {
    switch (state) {
      case 'BETTING':
      case 'BET_STARTING':
        return 'positive';
      case 'BET_CLOSED':
        return 'warning';
      case 'FINISHED':
        return 'neutral';
      default:
        return 'info';
    }
  }

  protected setSection(section: AdminSection): void {
    this.activeSection.set(section);
    this.deleteTarget.set(null);
    this.clearMessages();
  }

  protected async refreshAll(): Promise<void> {
    await this.runAction('Chargement des donnees admin...', async () => {
      const [horses, races, entries] = await Promise.all([
        this.adminApi.findHorses(),
        this.adminApi.findRaces(),
        this.adminApi.findRaceEntries(this.selectedRaceId())
      ]);

      this.horses.set(horses);
      this.races.set(races);
      this.raceEntries.set(entries);
    });
  }

  protected async filterEntriesByRace(raceId: string): Promise<void> {
    this.selectedRaceId.set(this.parseNullableNumber(raceId));
    await this.runAction('Chargement des participations...', async () => {
      this.raceEntries.set(await this.adminApi.findRaceEntries(this.selectedRaceId()));
    });
  }

  protected async submitHorse(): Promise<void> {
    if (this.horseForm.invalid) {
      this.horseForm.markAllAsTouched();
      return;
    }

    const request: HorseAdminRequest = {
      name: this.horseForm.controls.name.value.trim()
    };

    await this.runAction('Enregistrement du cheval...', async () => {
      const id = this.editingHorseId();

      if (id === null) {
        await this.adminApi.createHorse(request);
        this.success.set('Cheval cree.');
      } else {
        await this.adminApi.updateHorse(id, request);
        this.success.set('Cheval mis a jour.');
      }

      this.cancelHorseEdit();
      this.horses.set(await this.adminApi.findHorses());
    });
  }

  protected editHorse(horse: HorseAdminResponse): void {
    this.editingHorseId.set(horse.id);
    this.horseForm.setValue({ name: horse.name });
    this.setSection('horses');
  }

  protected cancelHorseEdit(): void {
    this.editingHorseId.set(null);
    this.horseForm.reset({ name: '' });
  }

  protected requestHorseDelete(horse: HorseAdminResponse): void {
    this.deleteTarget.set({ type: 'horse', id: horse.id, label: horse.name });
  }

  private async deleteHorse(id: number): Promise<void> {
    await this.runAction('Suppression du cheval...', async () => {
      await this.adminApi.deleteHorse(id);
      this.horses.set(await this.adminApi.findHorses());
      this.success.set('Cheval supprime.');
    });
  }

  protected async submitRace(): Promise<void> {
    if (this.raceForm.invalid) {
      this.raceForm.markAllAsTouched();
      return;
    }

    const request: RaceAdminRequest = {
      name: this.raceForm.controls.name.value.trim(),
      raceImgUrl: this.normalizeOptionalText(this.raceForm.controls.raceImgUrl.value),
      state: this.raceForm.controls.state.value
    };

    await this.runAction('Enregistrement de la course...', async () => {
      const id = this.editingRaceId();

      if (id === null) {
        await this.adminApi.createRace(request);
        this.success.set('Course creee.');
      } else {
        await this.adminApi.updateRace(id, request);
        this.success.set('Course mise a jour.');
      }

      this.cancelRaceEdit();
      this.races.set(await this.adminApi.findRaces());
    });
  }

  protected editRace(race: RaceAdminResponse): void {
    this.editingRaceId.set(race.id);
    this.raceForm.setValue({
      name: race.name,
      raceImgUrl: race.raceImgUrl,
      state: race.state
    });
    this.setSection('races');
  }

  protected cancelRaceEdit(): void {
    this.editingRaceId.set(null);
    this.raceForm.reset({ name: '', raceImgUrl: null, state: 'CREATED' });
  }

  protected requestRaceDelete(race: RaceAdminResponse): void {
    this.deleteTarget.set({ type: 'race', id: race.id, label: race.name });
  }

  private async deleteRace(id: number): Promise<void> {
    await this.runAction('Suppression de la course...', async () => {
      await this.adminApi.deleteRace(id);
      this.races.set(await this.adminApi.findRaces());
      this.success.set('Course supprimee.');
    });
  }

  protected async submitRaceEntry(): Promise<void> {
    if (this.raceEntryForm.invalid) {
      this.raceEntryForm.markAllAsTouched();
      return;
    }

    const request = this.buildRaceEntryRequest();

    if (request === null) {
      return;
    }

    await this.runAction('Enregistrement de la participation...', async () => {
      const id = this.editingRaceEntryId();

      if (id === null) {
        await this.adminApi.createRaceEntry(request);
        this.success.set('Participation creee.');
      } else {
        await this.adminApi.updateRaceEntry(id, request);
        this.success.set('Participation mise a jour.');
      }

      this.cancelRaceEntryEdit();
      this.raceEntries.set(await this.adminApi.findRaceEntries(this.selectedRaceId()));
    });
  }

  protected editRaceEntry(entry: RaceEntryAdminResponse): void {
    this.editingRaceEntryId.set(entry.id);
    this.raceEntryForm.setValue({
      raceId: entry.raceId,
      horseId: entry.horseId,
      horseNumber: entry.horseNumber,
      rank: entry.rank
    });
    this.setSection('entries');
  }

  protected cancelRaceEntryEdit(): void {
    this.editingRaceEntryId.set(null);
    this.raceEntryForm.reset({ raceId: null, horseId: null, horseNumber: null, rank: null });
  }

  protected requestRaceEntryDelete(entry: RaceEntryAdminResponse): void {
    this.deleteTarget.set({
      type: 'raceEntry',
      id: entry.id,
      label: `${entry.raceName} / dossard ${entry.horseNumber} / ${entry.horseName}`
    });
  }

  private async deleteRaceEntry(id: number): Promise<void> {
    await this.runAction('Suppression de la participation...', async () => {
      await this.adminApi.deleteRaceEntry(id);
      this.raceEntries.set(await this.adminApi.findRaceEntries(this.selectedRaceId()));
      this.success.set('Participation supprimee.');
    });
  }

  protected cancelDelete(): void {
    this.deleteTarget.set(null);
  }

  protected async confirmDelete(): Promise<void> {
    const target = this.deleteTarget();

    if (target === null) {
      return;
    }

    this.deleteTarget.set(null);

    if (target.type === 'horse') {
      await this.deleteHorse(target.id);
      return;
    }

    if (target.type === 'race') {
      await this.deleteRace(target.id);
      return;
    }

    await this.deleteRaceEntry(target.id);
  }

  private buildRaceEntryRequest(): RaceEntryAdminRequest | null {
    const raceId = this.raceEntryForm.controls.raceId.value;
    const horseId = this.raceEntryForm.controls.horseId.value;
    const horseNumber = this.raceEntryForm.controls.horseNumber.value;

    if (raceId === null || horseId === null || horseNumber === null) {
      return null;
    }

    return {
      raceId,
      horseId,
      horseNumber,
      rank: this.raceEntryForm.controls.rank.value
    };
  }

  private async runAction(label: string, action: () => Promise<void>): Promise<void> {
    this.loading.set(true);
    this.loadingLabel.set(label);
    this.clearMessages();

    try {
      await action();
    } catch (error: unknown) {
      this.error.set(this.toErrorMessage(error));
    } finally {
      this.loading.set(false);
      this.loadingLabel.set(null);
    }
  }

  private clearMessages(): void {
    this.error.set(null);
    this.success.set(null);
  }

  private normalizeOptionalText(value: string | null): string | null {
    if (value === null || value.trim() === '') {
      return null;
    }

    return value.trim();
  }

  private parseNullableNumber(value: string): number | null {
    return value === '' ? null : Number(value);
  }

  private toErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401 || error.status === 403) {
        return 'Acces admin refuse. Connectez-vous avec un compte ADMIN.';
      }

      if (error.status === 409) {
        return this.backendMessage(error) ?? 'Conflit metier: cette ressource existe deja ou viole une contrainte.';
      }

      if (error.status === 404) {
        return this.backendMessage(error) ?? 'Ressource introuvable.';
      }
    }

    return 'Operation impossible pour le moment.';
  }

  private backendMessage(error: HttpErrorResponse): string | null {
    if (this.isBackendErrorResponse(error.error)) {
      return error.error.message;
    }

    return null;
  }

  private isBackendErrorResponse(value: unknown): value is BackendErrorResponse {
    return typeof value === 'object' && value !== null && typeof (value as Record<string, unknown>)['message'] === 'string';
  }
}
