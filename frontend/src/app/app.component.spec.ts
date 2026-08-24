import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideTaiga } from '@taiga-ui/core';

import { AppComponent } from './app.component';
import { AuthService } from './core/auth/auth.service';

describe('AppComponent', () => {
  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideTaiga()]
    }).compileComponents();
  });

  afterEach(() => localStorage.clear());

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('hides navigation while signed out and reveals it immediately after login', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    const auth = TestBed.inject(AuthService);
    const http = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.app-topbar')).toBeNull();
    expect(fixture.nativeElement.querySelector('.mobile-nav')).toBeNull();

    const login = auth.login({ email: 'bettor@example.com', accessCode: 'access-code' });
    http.expectOne('/api/auth/login').flush({
      token: 'jwt-token',
      tokenType: 'Bearer',
      expiresIn: 86_400_000,
      roles: ['USER'],
      user: {
        id: 1,
        name: 'Camille',
        surname: 'Martin',
        birthDate: null,
        email: 'bettor@example.com',
        present: true,
        tutorialCompleted: false,
        roles: ['USER']
      }
    });
    await login;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.app-topbar')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.mobile-nav')).not.toBeNull();
  });

  it('redirects to login after logout even from the betting page', async () => {
    localStorage.setItem('racebets.auth.session', JSON.stringify({
      token: 'jwt-token',
      tokenType: 'Bearer',
      expiresAt: Date.now() + 86_400_000,
      roles: ['USER'],
      user: {
        id: 1,
        name: 'Camille',
        surname: 'Martin',
        birthDate: null,
        email: 'bettor@example.com',
        present: true,
        tutorialCompleted: true,
        roles: ['USER']
      }
    }));
    const fixture = TestBed.createComponent(AppComponent);
    const auth = TestBed.inject(AuthService);
    const router = TestBed.inject(Router);
    const navigate = spyOn(router, 'navigateByUrl').and.resolveTo(true);
    const actions = fixture.componentInstance as unknown as {confirmLogout(): Promise<void>};

    await actions.confirmLogout();

    expect(auth.isAuthenticated()).toBeFalse();
    expect(navigate).toHaveBeenCalledOnceWith('/login');
  });
});
