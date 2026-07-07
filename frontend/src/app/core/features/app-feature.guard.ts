import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AppFeaturesService } from './app-features.service';

export const bettingFeatureGuard: CanActivateFn = async (_route, state) => {
  const features = inject(AppFeaturesService);
  const router = inject(Router);
  const settings = await features.ensureLoaded();

  return settings.bettingEnabled ? true : redirectWithoutLoop(router, features.defaultPath(settings), state.url);
};

export const quizFeatureGuard: CanActivateFn = async (_route, state) => {
  const features = inject(AppFeaturesService);
  const router = inject(Router);
  const settings = await features.ensureLoaded();

  return settings.quizEnabled ? true : redirectWithoutLoop(router, features.defaultPath(settings), state.url);
};

function redirectWithoutLoop(router: Router, targetUrl: string, currentUrl: string) {
  return targetUrl === currentUrl ? router.createUrlTree(['/login']) : router.createUrlTree([targetUrl]);
}
