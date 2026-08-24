import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { provideTaiga } from '@taiga-ui/core';

import { AuthSession } from '../../../core/auth/auth.model';
import { AuthService } from '../../../core/auth/auth.service';
import { AppFeaturesService } from '../../../core/features/app-features.service';
import { TutorialService } from '../../../core/tutorial/tutorial.service';
import { LoginPageComponent } from './login-page.component';

describe('LoginPageComponent', () => {
  const session: AuthSession = {
    token: 'token',
    tokenType: 'Bearer',
    expiresAt: Date.now() + 60_000,
    roles: ['USER'],
    user: {
      id: 42,
      name: 'Camille',
      surname: 'Martin',
      birthDate: null,
      email: 'camille@example.com',
      present: true,
      tutorialCompleted: false,
      roles: ['USER']
    }
  };
  let auth: jasmine.SpyObj<AuthService>;
  let features: jasmine.SpyObj<AppFeaturesService>;
  let tutorial: jasmine.SpyObj<TutorialService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['login', 'register', 'providers', 'exchangeOAuthCode'], {
      isAdmin: signal(false)
    });
    auth.login.and.resolveTo(session);
    auth.register.and.resolveTo(session);
    auth.providers.and.resolveTo({ google: true });
    features = jasmine.createSpyObj<AppFeaturesService>('AppFeaturesService', ['ensureLoaded', 'defaultPath']);
    features.ensureLoaded.and.resolveTo({ bettingEnabled: true, quizEnabled: true });
    features.defaultPath.and.returnValue('/');
    tutorial = jasmine.createSpyObj<TutorialService>('TutorialService', ['shouldStart']);
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    router.navigateByUrl.and.resolveTo(true);

    await TestBed.configureTestingModule({
      imports: [LoginPageComponent],
      providers: [
        provideTaiga(),
        { provide: AuthService, useValue: auth },
        { provide: AppFeaturesService, useValue: features },
        { provide: TutorialService, useValue: tutorial },
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { get: () => null } } }
        }
      ]
    }).compileComponents();
  });

  it('opens the tutorial after the first login', async () => {
    tutorial.shouldStart.and.returnValue(true);
    const fixture = TestBed.createComponent(LoginPageComponent);
    fixture.componentInstance.form.setValue({ email: 'camille@example.com', accessCode: 'access-code' });

    await submit(fixture.componentInstance);

    expect(tutorial.shouldStart).toHaveBeenCalledOnceWith();
    expect(router.navigateByUrl).toHaveBeenCalledOnceWith('/tutorial');
  });

  it('uses the normal destination once the tutorial is completed', async () => {
    tutorial.shouldStart.and.returnValue(false);
    const fixture = TestBed.createComponent(LoginPageComponent);
    fixture.componentInstance.form.setValue({ email: 'camille@example.com', accessCode: 'access-code' });

    await submit(fixture.componentInstance);

    expect(router.navigateByUrl).toHaveBeenCalledOnceWith('/');
  });

  it('opens the tutorial immediately after registration', async () => {
    tutorial.shouldStart.and.returnValue(true);
    const fixture = TestBed.createComponent(LoginPageComponent);
    fixture.componentInstance.registerForm.setValue({
      name: 'Camille',
      surname: 'Martin',
      birthDate: null,
      email: 'camille@example.com',
      accessCode: 'access-code'
    });

    await register(fixture.componentInstance);

    expect(auth.register).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledOnceWith('/tutorial');
  });
});

function submit(component: LoginPageComponent): Promise<void> {
  return (component as unknown as { submit(): Promise<void> }).submit();
}

function register(component: LoginPageComponent): Promise<void> {
  return (component as unknown as { register(): Promise<void> }).register();
}
