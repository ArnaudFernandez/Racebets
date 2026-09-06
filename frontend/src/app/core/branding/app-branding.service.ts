import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { EMPTY, catchError, exhaustMap, firstValueFrom, tap, timeout, timer } from 'rxjs';

import { AppBrandingSettings, AppBrandingTheme } from './app-branding.model';

const DEFAULT_BRANDING: AppBrandingSettings = {
  appName: 'Racebets',
  imageUrl: '/logo_le_bouscat.png',
  loginTitle: 'Vivez la course, simplement.',
  loginSubtitle: 'Pariez en direct, suivez les résultats et retrouvez votre classement au même endroit.',
  passwordlessLoginEnabled: false,
  theme: 'DEFAULT'
};

@Injectable({ providedIn: 'root' })
export class AppBrandingService {
  private readonly http = inject(HttpClient);
  private readonly settingsState = signal<AppBrandingSettings>(DEFAULT_BRANDING);
  private readonly previewThemeState = signal<AppBrandingTheme | null>(null);
  private loadingPromise: Promise<AppBrandingSettings> | null = null;
  private loaded = false;
  private watching = false;
  private settingsVersion = 0;

  readonly settings = this.settingsState.asReadonly();
  readonly appName = computed(() => this.settingsState().appName);
  readonly imageUrl = computed(() => this.settingsState().imageUrl);
  readonly loginTitle = computed(() => this.settingsState().loginTitle);
  readonly loginSubtitle = computed(() => this.settingsState().loginSubtitle);
  readonly passwordlessLoginEnabled = computed(() => this.settingsState().passwordlessLoginEnabled);
  readonly theme = computed(() => this.previewThemeState() ?? this.settingsState().theme);

  async load(): Promise<AppBrandingSettings> {
    if (this.loadingPromise !== null) return this.loadingPromise;

    this.loadingPromise = firstValueFrom(
      this.http.get<AppBrandingSettings>('/api/app/branding').pipe(timeout(2500))
    )
      .then((settings) => {
        this.loaded = true;
        this.settingsState.set(settings);
        this.loadingPromise = null;
        return settings;
      })
      .catch((error: unknown) => {
        this.loadingPromise = null;
        throw error;
      });

    return this.loadingPromise;
  }

  apply(settings: AppBrandingSettings): void {
    this.settingsVersion++;
    this.loaded = true;
    this.previewThemeState.set(null);
    this.settingsState.set(settings);
  }

  previewTheme(theme: AppBrandingTheme): void {
    this.previewThemeState.set(theme);
  }

  clearThemePreview(): void {
    this.previewThemeState.set(null);
  }

  startWatching(): void {
    if (this.watching) return;
    this.watching = true;
    timer(0, 2000)
      .pipe(
        exhaustMap(() => {
          const requestVersion = this.settingsVersion;
          return this.http.get<AppBrandingSettings>('/api/app/branding').pipe(
            timeout(1500),
            tap((settings) => {
              if (requestVersion !== this.settingsVersion) return;
              this.loaded = true;
              this.settingsState.set(settings);
            }),
            catchError(() => EMPTY)
          );
        })
      )
      .subscribe();
  }

  async ensureLoaded(): Promise<AppBrandingSettings> {
    return this.loaded ? this.settingsState() : this.load();
  }
}
