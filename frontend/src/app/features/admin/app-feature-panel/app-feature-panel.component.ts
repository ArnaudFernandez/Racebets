import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { TuiButton, TuiDialog, TuiTitle } from '@taiga-ui/core';
import { TuiCard, TuiHeader } from '@taiga-ui/layout';

import { AppMode } from '../../../core/features/app-features.model';
import { AppFeaturesService } from '../../../core/features/app-features.service';

interface BackendErrorResponse {
  readonly message: string;
}

@Component({
  selector: 'app-feature-panel',
  imports: [TuiButton, TuiCard, TuiDialog, TuiHeader, TuiTitle],
  templateUrl: './app-feature-panel.component.html',
  styleUrl: './app-feature-panel.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppFeaturePanelComponent {
  private readonly features = inject(AppFeaturesService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly activeMode = signal<AppMode>('BETTING');
  readonly pendingMode = signal<AppMode | null>(null);
  readonly modeOptions: readonly { readonly mode: AppMode; readonly title: string; readonly description: string; readonly marker: string }[] = [
    { mode: 'BETTING', title: 'Paris fictifs', description: 'Courses, prises de paris et résultats en direct.', marker: '01' },
    { mode: 'QUIZ', title: 'Quiz live', description: 'Questions chronométrées et classement collectif.', marker: '02' },
    { mode: 'WORD_CLOUD', title: 'Nuage de mots', description: 'Réponses libres puis révélation des idées du public.', marker: '03' }
  ];

  constructor() {
    void this.load();
  }

  protected async load(): Promise<void> {
    await this.run(async () => {
      const settings = await this.features.ensureLoaded();
      this.activeMode.set(settings.activeMode);
    }, false);
  }

  protected requestMode(mode: AppMode): void {
    if (mode === this.activeMode() || this.loading()) return;
    this.pendingMode.set(mode);
    this.error.set(null);
    this.success.set(null);
  }

  protected cancelModeChange(): void {
    this.pendingMode.set(null);
  }

  protected async confirmModeChange(): Promise<void> {
    const mode = this.pendingMode();
    if (mode === null) return;
    this.pendingMode.set(null);
    await this.run(async () => {
      const settings = await this.features.update({ activeMode: mode });
      this.activeMode.set(settings.activeMode);
      this.success.set('Le nouvel affichage est actif pour tous les participants.');
    });
  }

  protected modeTitle(mode: AppMode): string {
    return this.modeOptions.find((option) => option.mode === mode)?.title ?? mode;
  }

  private async run(action: () => Promise<void>, clearSuccess = true): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    if (clearSuccess) this.success.set(null);
    try {
      await action();
    } catch (error: unknown) {
      this.error.set(this.toErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private toErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && this.isBackendError(error.error)) {
      return error.error.message;
    }
    return 'Impossible de mettre a jour l affichage pour le moment.';
  }

  private isBackendError(value: unknown): value is BackendErrorResponse {
    return typeof value === 'object' && value !== null && typeof (value as Record<string, unknown>)['message'] === 'string';
  }
}
