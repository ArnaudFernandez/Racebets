import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TuiButton, TuiTitle } from '@taiga-ui/core';
import { TuiCard, TuiHeader } from '@taiga-ui/layout';
import { TuiSwitch } from '@taiga-ui/kit';

import { AppFeaturesService } from '../../../core/features/app-features.service';

interface BackendErrorResponse {
  readonly message: string;
}

@Component({
  selector: 'app-feature-panel',
  imports: [ReactiveFormsModule, TuiButton, TuiCard, TuiHeader, TuiSwitch, TuiTitle],
  templateUrl: './app-feature-panel.component.html',
  styleUrl: './app-feature-panel.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppFeaturePanelComponent {
  private readonly features = inject(AppFeaturesService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  readonly form = new FormGroup({
    bettingEnabled: new FormControl(true, { nonNullable: true }),
    quizEnabled: new FormControl(true, { nonNullable: true })
  });

  constructor() {
    void this.load();
  }

  protected async load(): Promise<void> {
    await this.run(async () => {
      const settings = await this.features.ensureLoaded();
      this.form.setValue(settings);
    }, false);
  }

  protected async submit(): Promise<void> {
    const settings = this.form.getRawValue();
    if (!settings.bettingEnabled && !settings.quizEnabled) {
      this.error.set('Gardez au moins une partie active.');
      return;
    }

    await this.run(async () => {
      this.form.setValue(await this.features.update(settings));
      this.success.set('Affichage de l application mis a jour.');
    });
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
