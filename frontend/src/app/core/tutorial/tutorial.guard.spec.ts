import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';

import { tutorialGuard } from './tutorial.guard';
import { TutorialService } from './tutorial.service';
import { AppFeaturesService } from '../features/app-features.service';

describe('tutorialGuard', () => {
  let tutorial: jasmine.SpyObj<TutorialService>;
  let features: jasmine.SpyObj<AppFeaturesService>;

  beforeEach(() => {
    tutorial = jasmine.createSpyObj<TutorialService>('TutorialService', ['shouldStart']);
    features = jasmine.createSpyObj<AppFeaturesService>('AppFeaturesService', ['ensureLoaded', 'defaultPath']);
    features.ensureLoaded.and.resolveTo({ activeMode: 'BETTING' });
    features.defaultPath.and.returnValue('/');
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: TutorialService, useValue: tutorial },
        { provide: AppFeaturesService, useValue: features }
      ]
    });
  });

  it('redirects an authenticated user whose tutorial is not completed', async () => {
    tutorial.shouldStart.and.returnValue(true);

    const result = await runGuard('/');

    expect(result instanceof UrlTree).toBeTrue();
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/tutorial');
  });

  it('allows the tutorial route without creating a redirect loop', async () => {
    tutorial.shouldStart.and.returnValue(true);

    expect(await runGuard('/tutorial')).toBeTrue();
  });

  it('does not allow the betting tutorial in word cloud mode', async () => {
    features.ensureLoaded.and.resolveTo({ activeMode: 'WORD_CLOUD' });
    features.defaultPath.and.returnValue('/word-cloud');

    const result = await runGuard('/tutorial');

    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/word-cloud');
  });
});

function runGuard(url: string) {
  return TestBed.runInInjectionContext(() =>
    tutorialGuard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot)
  );
}
