import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { AuthService } from '../auth/auth.service';
import { AppFeaturesService } from '../features/app-features.service';
import { TutorialService } from './tutorial.service';

describe('TutorialService', () => {
  let service: TutorialService;
  let auth: jasmine.SpyObj<AuthService>;
  const user = signal(userProfile(false));
  const activeMode = signal<'BETTING' | 'QUIZ' | 'WORD_CLOUD'>('BETTING');

  beforeEach(() => {
    user.set(userProfile(false));
    activeMode.set('BETTING');
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['completeTutorial'], { user });
    auth.completeTutorial.and.callFake(async () => {
      const completed = userProfile(true);
      user.set(completed);
      return completed;
    });
    TestBed.configureTestingModule({ providers: [
      { provide: AuthService, useValue: auth },
      { provide: AppFeaturesService, useValue: { activeMode } }
    ] });
    service = TestBed.inject(TutorialService);
  });

  it('uses the server profile and persists completion through auth', async () => {
    expect(service.shouldStart()).toBeTrue();

    await service.complete();

    expect(auth.completeTutorial).toHaveBeenCalledOnceWith();
    expect(service.shouldStart()).toBeFalse();
  });

  it('does not start outside fictional betting mode', () => {
    activeMode.set('WORD_CLOUD');

    expect(service.shouldStart()).toBeFalse();
  });
});

function userProfile(tutorialCompleted: boolean) {
  return {
    id: 12,
    name: 'Camille',
    surname: 'Martin',
    birthDate: null,
    email: 'camille@example.com',
    present: true,
    tutorialCompleted,
    roles: ['USER'] as const
  };
}
