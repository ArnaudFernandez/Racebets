import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { AuthService } from '../auth/auth.service';
import { TutorialService } from './tutorial.service';

describe('TutorialService', () => {
  let service: TutorialService;
  let auth: jasmine.SpyObj<AuthService>;
  const user = signal(userProfile(false));

  beforeEach(() => {
    user.set(userProfile(false));
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['completeTutorial'], { user });
    auth.completeTutorial.and.callFake(async () => {
      const completed = userProfile(true);
      user.set(completed);
      return completed;
    });
    TestBed.configureTestingModule({ providers: [{ provide: AuthService, useValue: auth }] });
    service = TestBed.inject(TutorialService);
  });

  it('uses the server profile and persists completion through auth', async () => {
    expect(service.shouldStart()).toBeTrue();

    await service.complete();

    expect(auth.completeTutorial).toHaveBeenCalledOnceWith();
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
