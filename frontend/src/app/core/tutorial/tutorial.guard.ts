import { inject } from '@angular/core';
import { CanActivateChildFn, Router } from '@angular/router';

import { TutorialService } from './tutorial.service';

export const tutorialGuard: CanActivateChildFn = (_route, state) => {
  const tutorial = inject(TutorialService);
  const router = inject(Router);
  const path = state.url.split('?')[0];

  return path === '/tutorial' || !tutorial.shouldStart()
    ? true
    : router.createUrlTree(['/tutorial']);
};
