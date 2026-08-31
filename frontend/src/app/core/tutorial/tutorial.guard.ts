import { inject } from '@angular/core';
import { CanActivateChildFn, Router } from '@angular/router';

import { TutorialService } from './tutorial.service';
import { AppFeaturesService } from '../features/app-features.service';

export const tutorialGuard: CanActivateChildFn = async (_route, state) => {
  const tutorial = inject(TutorialService);
  const features = inject(AppFeaturesService);
  const router = inject(Router);
  const path = state.url.split('?')[0];
  const settings = await features.ensureLoaded();

  if (path === '/tutorial') {
    return settings.activeMode === 'BETTING'
      ? true
      : router.createUrlTree([features.defaultPath(settings)]);
  }

  return tutorial.shouldStart() ? router.createUrlTree(['/tutorial']) : true;
};
