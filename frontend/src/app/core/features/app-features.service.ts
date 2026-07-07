import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { catchError, firstValueFrom, of, timeout } from 'rxjs';

import { AppFeatureSettings } from './app-features.model';

const DEFAULT_FEATURES: AppFeatureSettings = {
  bettingEnabled: true,
  quizEnabled: true
};

@Injectable({ providedIn: 'root' })
export class AppFeaturesService {
  private readonly http = inject(HttpClient);
  private readonly settingsState = signal<AppFeatureSettings>(DEFAULT_FEATURES);
  private loadingPromise: Promise<AppFeatureSettings> | null = null;
  private loaded = false;

  readonly settings = this.settingsState.asReadonly();
  readonly bettingEnabled = computed(() => this.settingsState().bettingEnabled);
  readonly quizEnabled = computed(() => this.settingsState().quizEnabled);

  async load(): Promise<AppFeatureSettings> {
    if (this.loadingPromise !== null) {
      return this.loadingPromise;
    }

    this.loadingPromise = firstValueFrom(
      this.http.get<AppFeatureSettings>('/api/app/features').pipe(
        timeout(2500),
        catchError(() => of(DEFAULT_FEATURES))
      )
    ).then((settings) => {
      this.loaded = true;
      this.settingsState.set(settings);
      this.loadingPromise = null;
      return settings;
    });

    return this.loadingPromise;
  }

  async ensureLoaded(): Promise<AppFeatureSettings> {
    return this.loaded ? this.settingsState() : this.load();
  }

  async update(settings: AppFeatureSettings): Promise<AppFeatureSettings> {
    const updated = await firstValueFrom(this.http.put<AppFeatureSettings>('/api/admin/app/features', settings));
    this.loaded = true;
    this.settingsState.set(updated);
    return updated;
  }

  defaultPath(settings = this.settingsState()): string {
    if (settings.bettingEnabled) {
      return '/';
    }

    if (settings.quizEnabled) {
      return '/quiz';
    }

    return '/login';
  }
}
