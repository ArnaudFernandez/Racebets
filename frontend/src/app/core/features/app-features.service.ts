import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { EMPTY, catchError, exhaustMap, firstValueFrom, tap, timeout, timer } from 'rxjs';

import { AppFeatureSettings, AppMode } from './app-features.model';

const DEFAULT_FEATURES: AppFeatureSettings = {
  activeMode: 'BETTING'
};

@Injectable({ providedIn: 'root' })
export class AppFeaturesService {
  private readonly http = inject(HttpClient);
  private readonly settingsState = signal<AppFeatureSettings>(DEFAULT_FEATURES);
  private readonly readyState = signal(false);
  private loadingPromise: Promise<AppFeatureSettings> | null = null;
  private loaded = false;
  private watching = false;
  private settingsVersion = 0;

  readonly settings = this.settingsState.asReadonly();
  readonly ready = this.readyState.asReadonly();
  readonly activeMode = computed(() => this.settingsState().activeMode);
  readonly bettingEnabled = computed(() => this.activeMode() === 'BETTING');
  readonly quizEnabled = computed(() => this.activeMode() === 'QUIZ');
  readonly wordCloudEnabled = computed(() => this.activeMode() === 'WORD_CLOUD');

  async load(): Promise<AppFeatureSettings> {
    if (this.loadingPromise !== null) {
      return this.loadingPromise;
    }

    this.loadingPromise = firstValueFrom(
      this.http.get<AppFeatureSettings>('/api/app/features').pipe(timeout(2500))
    ).then((settings) => {
      this.loaded = true;
      this.readyState.set(true);
      this.settingsState.set(settings);
      this.loadingPromise = null;
      return settings;
    }).catch((error: unknown) => {
      this.loadingPromise = null;
      throw error;
    });

    return this.loadingPromise;
  }

  async ensureLoaded(): Promise<AppFeatureSettings> {
    return this.loaded ? this.settingsState() : this.load();
  }

  async update(settings: AppFeatureSettings): Promise<AppFeatureSettings> {
    const updated = await firstValueFrom(this.http.put<AppFeatureSettings>('/api/admin/app/features', settings));
    this.settingsVersion++;
    this.loaded = true;
    this.readyState.set(true);
    this.settingsState.set(updated);
    return updated;
  }

  startWatching(): void {
    if (this.watching) return;
    this.watching = true;
    timer(0, 2000)
      .pipe(
        exhaustMap(() => {
          const requestVersion = this.settingsVersion;
          return this.http.get<AppFeatureSettings>('/api/app/features').pipe(
            timeout(1500),
            tap((settings) => {
              if (requestVersion !== this.settingsVersion) return;
              this.loaded = true;
              this.readyState.set(true);
              this.settingsState.set(settings);
            }),
            catchError(() => EMPTY)
          );
        })
      )
      .subscribe();
  }

  defaultPath(settings = this.settingsState()): string {
    return this.pathForMode(settings.activeMode);
  }

  pathForMode(mode: AppMode): string {
    return ({ BETTING: '/', QUIZ: '/quiz', WORD_CLOUD: '/word-cloud' })[mode];
  }
}
