import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnDestroy, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TuiButton, TuiInput, TuiLoader, TuiTitle } from '@taiga-ui/core';
import { TuiCard, TuiHeader } from '@taiga-ui/layout';

import { AppBrandingService } from '../../../core/branding/app-branding.service';
import { AppBrandingTheme } from '../../../core/branding/app-branding.model';
import { AdminApiService } from '../services/admin-api.service';

@Component({
  selector: 'app-branding-panel',
  imports: [ReactiveFormsModule, TuiButton, TuiCard, TuiHeader, TuiInput, TuiLoader, TuiTitle],
  templateUrl: './app-branding-panel.component.html',
  styleUrl: './app-branding-panel.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppBrandingPanelComponent implements OnDestroy {
  private readonly adminApi = inject(AdminApiService);
  protected readonly branding = inject(AppBrandingService);

  readonly selectedImage = signal<File | null>(null);
  readonly selectedTheme = signal<AppBrandingTheme>('DEFAULT');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  readonly form = new FormGroup({
    appName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(120)]
    }),
    loginTitle: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(160)]
    }),
    loginSubtitle: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(300)]
    })
  });

  constructor() {
    void this.load();
  }

  ngOnDestroy(): void {
    this.branding.clearThemePreview();
  }

  protected async load(): Promise<void> {
    await this.run(async () => {
      const settings = await this.adminApi.findBranding();
      this.branding.apply(settings);
      this.form.reset({
        appName: settings.appName,
        loginTitle: settings.loginTitle,
        loginSubtitle: settings.loginSubtitle
      });
      this.selectedTheme.set(settings.theme);
    }, false);
  }

  protected selectImage(event: Event): void {
    const input = event.target as HTMLInputElement;
    const image = input.files?.item(0) ?? null;
    if (
      image !== null &&
      (!['image/png', 'image/jpeg', 'image/webp'].includes(image.type) ||
        image.size > 2 * 1024 * 1024)
    ) {
      this.selectedImage.set(null);
      this.error.set('Choisissez une image PNG, JPEG ou WebP de 2 Mo maximum.');
      input.value = '';
      return;
    }
    this.selectedImage.set(image);
    this.error.set(null);
  }

  protected selectTheme(theme: AppBrandingTheme): void {
    this.selectedTheme.set(theme);
    this.branding.previewTheme(theme);
  }

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    await this.run(async () => {
      const data = new FormData();
      data.append('appName', this.form.controls.appName.value.trim());
      data.append('loginTitle', this.form.controls.loginTitle.value.trim());
      data.append('loginSubtitle', this.form.controls.loginSubtitle.value.trim());
      data.append('theme', this.selectedTheme());
      const image = this.selectedImage();
      if (image !== null) data.append('image', image);

      const settings = await this.adminApi.updateBranding(data);
      this.branding.apply(settings);
      this.form.reset({
        appName: settings.appName,
        loginTitle: settings.loginTitle,
        loginSubtitle: settings.loginSubtitle
      });
      this.selectedTheme.set(settings.theme);
      this.selectedImage.set(null);
      this.success.set('Le branding de l’application a été mis à jour.');
    });
  }

  protected resetImageSelection(): void {
    this.selectedImage.set(null);
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
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message;
    }
    if (error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403)) {
      return 'Accès admin refusé.';
    }
    return 'Impossible de mettre à jour le branding pour le moment.';
  }
}
