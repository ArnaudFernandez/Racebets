import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AppBrandingService } from './app-branding.service';

describe('AppBrandingService', () => {
  let service: AppBrandingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AppBrandingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the public branding settings', async () => {
    const loading = service.load();
    http
      .expectOne('/api/app/branding')
      .flush({
        appName: 'Hippodrome',
        imageUrl: '/api/app/branding/image?v=2',
        loginTitle: 'Vibrez ensemble',
        loginSubtitle: 'Une expérience en direct.',
        passwordlessLoginEnabled: true,
        theme: 'OLIFAN_GROUP'
      });

    await expectAsync(loading).toBeResolvedTo({
      appName: 'Hippodrome',
      imageUrl: '/api/app/branding/image?v=2',
      loginTitle: 'Vibrez ensemble',
      loginSubtitle: 'Une expérience en direct.',
      passwordlessLoginEnabled: true,
      theme: 'OLIFAN_GROUP'
    });
    expect(service.appName()).toBe('Hippodrome');
    expect(service.imageUrl()).toBe('/api/app/branding/image?v=2');
    expect(service.loginTitle()).toBe('Vibrez ensemble');
    expect(service.loginSubtitle()).toBe('Une expérience en direct.');
    expect(service.passwordlessLoginEnabled()).toBeTrue();
    expect(service.theme()).toBe('OLIFAN_GROUP');
  });

  it('applies branding updates immediately', () => {
    service.apply({
      appName: 'Grand Prix',
      imageUrl: '/brand.png',
      loginTitle: 'À vos marques',
      loginSubtitle: 'Participez maintenant.',
      passwordlessLoginEnabled: false,
      theme: 'DEFAULT'
    });

    expect(service.settings()).toEqual({
      appName: 'Grand Prix',
      imageUrl: '/brand.png',
      loginTitle: 'À vos marques',
      loginSubtitle: 'Participez maintenant.',
      passwordlessLoginEnabled: false,
      theme: 'DEFAULT'
    });
  });

  it('previews a theme without replacing persisted branding', () => {
    service.previewTheme('OLIFAN_GROUP');

    expect(service.theme()).toBe('OLIFAN_GROUP');
    expect(service.settings().theme).toBe('DEFAULT');

    service.clearThemePreview();

    expect(service.theme()).toBe('DEFAULT');
  });

  it('keeps passwordless login disabled before public settings load', () => {
    expect(service.passwordlessLoginEnabled()).toBeFalse();
  });
});
