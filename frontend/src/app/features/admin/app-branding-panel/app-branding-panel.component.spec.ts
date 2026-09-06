import { signal } from '@angular/core';
import { TestBed, fakeAsync, flushMicrotasks } from '@angular/core/testing';
import { provideTaiga } from '@taiga-ui/core';

import { AppBrandingSettings } from '../../../core/branding/app-branding.model';
import { AppBrandingService } from '../../../core/branding/app-branding.service';
import { AdminApiService } from '../services/admin-api.service';
import { AppBrandingPanelComponent } from './app-branding-panel.component';

describe('AppBrandingPanelComponent', () => {
  const disabledSettings: AppBrandingSettings = {
    appName: 'Racebets',
    imageUrl: '/logo.png',
    loginTitle: 'Vivez la course',
    loginSubtitle: 'Participez à l’événement.',
    passwordlessLoginEnabled: false,
    theme: 'DEFAULT'
  };
  const enabledSettings: AppBrandingSettings = {
    ...disabledSettings,
    passwordlessLoginEnabled: true
  };
  let adminApi: jasmine.SpyObj<AdminApiService>;
  let branding: jasmine.SpyObj<AppBrandingService>;

  beforeEach(async () => {
    adminApi = jasmine.createSpyObj<AdminApiService>('AdminApiService', [
      'findBranding',
      'updateBranding'
    ]);
    adminApi.findBranding.and.resolveTo(disabledSettings);
    adminApi.updateBranding.and.resolveTo(enabledSettings);
    branding = jasmine.createSpyObj<AppBrandingService>(
      'AppBrandingService',
      ['apply', 'previewTheme', 'clearThemePreview'],
      {
        imageUrl: signal('/logo.png'),
        appName: signal('Racebets'),
        loginTitle: signal('Vivez la course'),
        loginSubtitle: signal('Participez à l’événement.')
      }
    );

    await TestBed.configureTestingModule({
      imports: [AppBrandingPanelComponent],
      providers: [
        provideTaiga(),
        { provide: AdminApiService, useValue: adminApi },
        { provide: AppBrandingService, useValue: branding }
      ]
    }).compileComponents();
  });

  it('requires explicit confirmation before enabling passwordless login', fakeAsync(() => {
    const fixture = TestBed.createComponent(AppBrandingPanelComponent);
    flushMicrotasks();
    const component = fixture.componentInstance;

    component.form.controls.passwordlessLoginEnabled.setValue(true);
    passwordlessLoginChanged(component, true);

    expect(component.passwordlessConfirmationOpen()).toBeTrue();
    expect(adminApi.updateBranding).not.toHaveBeenCalled();

    cancelPasswordlessActivation(component);
    expect(component.form.controls.passwordlessLoginEnabled.value).toBeFalse();
    expect(component.passwordlessConfirmationOpen()).toBeFalse();
  }));

  it('persists the confirmed passwordless login setting', fakeAsync(() => {
    const fixture = TestBed.createComponent(AppBrandingPanelComponent);
    flushMicrotasks();
    const component = fixture.componentInstance;

    component.form.controls.passwordlessLoginEnabled.setValue(true);
    passwordlessLoginChanged(component, true);
    confirmPasswordlessActivation(component);
    void submit(component);
    flushMicrotasks();

    const data = adminApi.updateBranding.calls.mostRecent().args[0];
    expect(data.get('passwordlessLoginEnabled')).toBe('true');
    expect(branding.apply).toHaveBeenCalledWith(enabledSettings);
    expect(component.passwordlessConfirmationOpen()).toBeFalse();
  }));

  it('allows passwordless login to be disabled without confirmation', fakeAsync(() => {
    adminApi.findBranding.and.resolveTo(enabledSettings);
    adminApi.updateBranding.and.resolveTo(disabledSettings);
    const fixture = TestBed.createComponent(AppBrandingPanelComponent);
    flushMicrotasks();
    const component = fixture.componentInstance;

    component.form.controls.passwordlessLoginEnabled.setValue(false);
    passwordlessLoginChanged(component, false);
    void submit(component);
    flushMicrotasks();

    expect(component.passwordlessConfirmationOpen()).toBeFalse();
    expect(adminApi.updateBranding.calls.mostRecent().args[0].get('passwordlessLoginEnabled'))
      .toBe('false');
  }));
});

function passwordlessLoginChanged(component: AppBrandingPanelComponent, checked: boolean): void {
  const event = { target: { checked } } as unknown as Event;
  (component as unknown as { passwordlessLoginChanged(event: Event): void })
    .passwordlessLoginChanged(event);
}

function cancelPasswordlessActivation(component: AppBrandingPanelComponent): void {
  (component as unknown as { cancelPasswordlessActivation(): void })
    .cancelPasswordlessActivation();
}

function confirmPasswordlessActivation(component: AppBrandingPanelComponent): void {
  (component as unknown as { confirmPasswordlessActivation(): void })
    .confirmPasswordlessActivation();
}

function submit(component: AppBrandingPanelComponent): Promise<void> {
  return (component as unknown as { submit(): Promise<void> }).submit();
}
