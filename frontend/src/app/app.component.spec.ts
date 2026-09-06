import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideTaiga } from '@taiga-ui/core';

import { AppComponent } from './app.component';
import { AuthService } from './core/auth/auth.service';
import { AppBrandingService } from './core/branding/app-branding.service';

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

  it('updates the document title and favicon when branding changes', () => {
    const favicon = document.createElement('link');
    favicon.rel = 'icon';
    document.head.appendChild(favicon);
    const themeColor = document.createElement('meta');
    themeColor.name = 'theme-color';
    document.head.appendChild(themeColor);
    const fixture = TestBed.createComponent(AppComponent);
    const branding = TestBed.inject(AppBrandingService);

    fixture.detectChanges();
    branding.apply({
      appName: 'Grand Prix',
      imageUrl: '/api/app/branding/image?v=3',
      loginTitle: 'Vibrez ensemble',
      loginSubtitle: 'Une expérience en direct.',
      passwordlessLoginEnabled: false,
      theme: 'OLIFAN_GROUP'
    });
    fixture.detectChanges();

    expect(document.title).toBe('Grand Prix');
    expect(favicon.getAttribute('href')).toBe('/api/app/branding/image?v=3');
    expect(document.documentElement.dataset['brandTheme']).toBe('olifan-group');
    expect(themeColor.content).toBe('#8d1d22');
    const rootStyles = getComputedStyle(document.documentElement);
    expect(rootStyles.getPropertyValue('--rb-brand').trim()).toBe('#8d1d22');
    expect(rootStyles.getPropertyValue('--rb-positive').trim()).toBe('#8d1d22');
    favicon.remove();
    themeColor.remove();
  });

  it('previews and restores a branding theme immediately', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const branding = TestBed.inject(AppBrandingService);
    fixture.detectChanges();

    branding.previewTheme('OLIFAN_GROUP');
    fixture.detectChanges();

    expect(document.documentElement.dataset['brandTheme']).toBe('olifan-group');

    branding.clearThemePreview();
    fixture.detectChanges();

    expect(document.documentElement.dataset['brandTheme']).toBe('default');
  });
});
