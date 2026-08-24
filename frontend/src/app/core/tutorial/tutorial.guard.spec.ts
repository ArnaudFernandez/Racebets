import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';

import { tutorialGuard } from './tutorial.guard';
import { TutorialService } from './tutorial.service';

describe('tutorialGuard', () => {
  let tutorial: jasmine.SpyObj<TutorialService>;

  beforeEach(() => {
    tutorial = jasmine.createSpyObj<TutorialService>('TutorialService', ['shouldStart']);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: TutorialService, useValue: tutorial }]
    });
  });

  it('redirects an authenticated user whose tutorial is not completed', () => {
    tutorial.shouldStart.and.returnValue(true);

    const result = runGuard('/');

    expect(result instanceof UrlTree).toBeTrue();
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/tutorial');
  });

  it('allows the tutorial route without creating a redirect loop', () => {
    tutorial.shouldStart.and.returnValue(true);

    expect(runGuard('/tutorial')).toBeTrue();
  });
});

function runGuard(url: string) {
  return TestBed.runInInjectionContext(() =>
    tutorialGuard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot)
  );
}
