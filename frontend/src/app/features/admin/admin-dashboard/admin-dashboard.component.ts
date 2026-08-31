import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TuiTable } from '@taiga-ui/addon-table';
import { TuiButton, TuiDialog, TuiInput, TuiLoader, TuiTitle } from '@taiga-ui/core';
import { TuiCard, TuiHeader } from '@taiga-ui/layout';
import { TuiBadge, TuiTabs } from '@taiga-ui/kit';
import { ActivatedRoute, Router } from '@angular/router';

import { AdminUserPanelComponent } from '../admin-user-panel/admin-user-panel.component';
import { AppFeaturePanelComponent } from '../app-feature-panel/app-feature-panel.component';
import { AdminQuizPanelComponent } from '../../quiz/admin-quiz-panel/admin-quiz-panel.component';
import { AdminWordCloudPanelComponent } from '../../word-cloud/admin-word-cloud-panel/admin-word-cloud-panel.component';
import { AdminRaceHistoryPanelComponent } from '../admin-race-history-panel/admin-race-history-panel.component';
import { AdminPartnerPanelComponent } from '../admin-partner-panel/admin-partner-panel.component';
import { AppBrandingPanelComponent } from '../app-branding-panel/app-branding-panel.component';
import {
  HorseAdminRequest,
  HorseAdminResponse,
  RaceAdminRequest,
  RaceAdminResponse,
  RaceState
} from '../models/admin-api.model';
import { AdminApiService } from '../services/admin-api.service';

type AdminSection = 'features' | 'app-branding' | 'horses' | 'races' | 'history' | 'partners' | 'users' | 'quizzes' | 'word-cloud';
type DeleteTarget =
  | { readonly type: 'horse'; readonly id: number; readonly label: string }
  | { readonly type: 'race'; readonly id: number; readonly label: string };

interface BackendErrorResponse {
  readonly message: string;
}

@Component({
  selector: 'app-admin-dashboard',
  imports: [AppBrandingPanelComponent, AdminPartnerPanelComponent, AdminQuizPanelComponent, AdminRaceHistoryPanelComponent, AdminUserPanelComponent, AdminWordCloudPanelComponent, AppFeaturePanelComponent, ReactiveFormsModule, TuiBadge, TuiButton, TuiCard, TuiDialog, TuiHeader, TuiInput, TuiLoader, TuiTable, TuiTabs, TuiTitle],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminDashboardComponent {
  private readonly adminApi = inject(AdminApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly sections: readonly { readonly id: AdminSection; readonly label: string }[] = [
    { id: 'features', label: 'Affichage' },
    { id: 'app-branding', label: 'App branding' },
    { id: 'horses', label: 'Chevaux' },
    { id: 'races', label: 'Courses' },
    { id: 'history', label: 'Historique' },
    { id: 'partners', label: 'Partenaires' },
    { id: 'users', label: 'Utilisateurs' },
    { id: 'quizzes', label: 'Quiz' },
    { id: 'word-cloud', label: 'Nuage de mots' }
  ];

  readonly activeSection = signal<AdminSection>(this.initialSection());
  readonly activeSectionIndex = computed(() => this.sections.findIndex((section) => section.id === this.activeSection()));
  readonly horses = signal<readonly HorseAdminResponse[]>([]);
  readonly races = signal<readonly RaceAdminResponse[]>([]);
  readonly sortedHorses = computed(() => [...this.horses()].sort((left, right) => left.id - right.id));
  readonly sortedRaces = computed(() => [...this.races()].sort((left, right) => left.id - right.id));
  readonly loading = signal(false);
  readonly loadingLabel = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly deleteTarget = signal<DeleteTarget | null>(null);
  readonly editingHorseId = signal<number | null>(null);
  readonly editingRaceId = signal<number | null>(null);
  readonly selectedRaceImage = signal<File | null>(null);
  readonly raceDialogOpen = signal(false);

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

  constructor() {
    void this.refreshAll();
  }

  protected sectionCount(section: AdminSection): number | null {
    switch (section) {
      case 'horses':
        return this.horses().length;
      case 'races':
        return this.races().length;
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

  protected raceStateLabel(state: RaceState): string {
    return ({
      CREATED: 'Brouillon',
      STANDBY: 'À l’affiche',
      BET_STARTING: 'Ouverture imminente',
      BETTING: 'Paris ouverts',
      BET_CLOSED: 'Paris clos',
      FINISHED: 'Terminée'
    })[state];
  }

  protected setSection(section: AdminSection): void {
    this.activeSection.set(section);
    this.deleteTarget.set(null);
    this.clearMessages();
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { section: section === 'features' ? null : section },
      queryParamsHandling: 'merge'
    });
  }

  protected async refreshAll(): Promise<void> {
    await this.runAction('Chargement des donnees admin...', async () => {
      const [horses, races] = await Promise.all([
        this.adminApi.findHorses(),
        this.adminApi.findRaces()
      ]);

      this.horses.set(horses);
      this.races.set(races);
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
      let savedRace: RaceAdminResponse;

      if (id === null) {
        savedRace = await this.adminApi.createRace(request);
        this.success.set('Course creee.');
      } else {
        savedRace = await this.adminApi.updateRace(id, request);
        this.success.set('Course mise a jour.');
      }

      const image = this.selectedRaceImage();
      if (image !== null) {
        try {
          await this.adminApi.uploadRaceImage(savedRace.id, image);
        } catch (error: unknown) {
          this.success.set(id === null ? 'Course créée, mais son image n’a pas été ajoutée.' : 'Course mise à jour, mais son image n’a pas été ajoutée.');
          this.error.set(error instanceof HttpErrorResponse && error.status === 404
            ? 'Le serveur doit être redéployé avec la version qui gère l’upload des images.'
            : 'L’image n’a pas pu être ajoutée. La course reste enregistrée.');
        }
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
    this.raceDialogOpen.set(true);
  }

  protected openRaceCreation(): void {
    this.cancelRaceEdit();
    this.raceDialogOpen.set(true);
  }

  protected selectRaceImage(event: Event): void {
    const input = event.target as HTMLInputElement;
    const image = input.files?.item(0) ?? null;
    if (image !== null && (!['image/png', 'image/jpeg', 'image/webp'].includes(image.type) || image.size > 5 * 1024 * 1024)) {
      this.selectedRaceImage.set(null);
      this.error.set('Choisissez une image PNG, JPEG ou WebP de 5 Mo maximum.');
      input.value = '';
      return;
    }
    this.selectedRaceImage.set(image);
  }

  protected closeRaceDialog(): void {
    this.cancelRaceEdit();
  }

  protected openRace(race: RaceAdminResponse): void {
    void this.router.navigate(['/admin/races', race.id]);
  }

  protected cancelRaceEdit(): void {
    this.editingRaceId.set(null);
    this.selectedRaceImage.set(null);
    this.raceForm.reset({ name: '', raceImgUrl: null, state: 'CREATED' });
    this.raceDialogOpen.set(false);
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

    await this.deleteRace(target.id);
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

  private initialSection(): AdminSection {
    const section = this.route.snapshot.queryParamMap.get('section');
    return this.sections.some((candidate) => candidate.id === section) ? section as AdminSection : 'features';
  }
}
