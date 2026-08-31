import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AppMode } from './app-features.model';
import { AppFeaturesService } from './app-features.service';

function modeGuard(expectedMode: AppMode): CanActivateFn {
  return async (_route, state) => {
    const features = inject(AppFeaturesService);
    const router = inject(Router);
    const settings = await features.ensureLoaded();

    return settings.activeMode === expectedMode
      ? true
      : redirectWithoutLoop(router, features.defaultPath(settings), state.url);
  };
}

export const bettingFeatureGuard = modeGuard('BETTING');
export const quizFeatureGuard = modeGuard('QUIZ');
export const wordCloudFeatureGuard = modeGuard('WORD_CLOUD');

function redirectWithoutLoop(router: Router, targetUrl: string, currentUrl: string) {
  return targetUrl === currentUrl ? router.createUrlTree(['/login']) : router.createUrlTree([targetUrl]);
}
