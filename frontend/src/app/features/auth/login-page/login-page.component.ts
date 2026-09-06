import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TuiButton, TuiInput } from '@taiga-ui/core';
import { TuiCard } from '@taiga-ui/layout';

import { AuthService } from '../../../core/auth/auth.service';
import { AppBrandingService } from '../../../core/branding/app-branding.service';
import { AppFeaturesService } from '../../../core/features/app-features.service';
import { TutorialService } from '../../../core/tutorial/tutorial.service';
import { PublicPartner } from '../../betting/models/partner.model';
import { PartnerService } from '../../betting/services/partner.service';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, TuiButton, TuiCard, TuiInput],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoginPageComponent {
  private static readonly GOOGLE_RETURN_URL_KEY = 'racebets.google.returnUrl';

  private readonly auth = inject(AuthService);
  protected readonly branding = inject(AppBrandingService);
  private readonly features = inject(AppFeaturesService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly tutorial = inject(TutorialService);
  private readonly partnerService = inject(PartnerService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly mode = signal<'login' | 'register'>('login');
  readonly googleAvailable = signal(false);
  readonly googleLinkCode = signal<string | null>(null);
  readonly partners = signal<readonly PublicPartner[]>([]);
  readonly adminLogin = signal(false);
  readonly passwordRequired = computed(
    () => !this.branding.passwordlessLoginEnabled() || this.adminLogin()
  );

  readonly form = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email]
    }),
    accessCode: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(128)]
    })
  });

  readonly registerForm = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(80)]
    }),
    surname: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(80)]
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(180)]
    }),
    accessCode: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8), Validators.maxLength(128)]
    })
  });

  readonly googleLinkForm = new FormGroup({
    accessCode: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(128)]
    })
  });

  constructor() {
    void this.initializeGoogleAuthentication();
    void this.loadPartners();
  }

  protected setMode(mode: 'login' | 'register'): void {
    this.mode.set(mode);
    this.adminLogin.set(false);
    this.error.set(null);
  }

  protected useAdminLogin(): void {
    this.adminLogin.set(true);
    this.error.set(null);
  }

  protected useParticipantLogin(): void {
    this.adminLogin.set(false);
    this.form.controls.accessCode.reset();
    this.error.set(null);
  }

  protected async submit(): Promise<void> {
    const accessCode = this.form.controls.accessCode.value;
    if (this.form.invalid || (this.passwordRequired() && accessCode.trim().length === 0)) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    try {
      const email = this.form.controls.email.value.trim();
      await this.auth.login(this.passwordRequired() ? { email, accessCode } : { email });

      await this.navigateAfterAuthentication();
    } catch (error: unknown) {
      this.error.set(this.toErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  protected async register(): Promise<void> {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    try {
      await this.auth.register({
        name: this.registerForm.controls.name.value.trim(),
        surname: this.registerForm.controls.surname.value.trim(),
        email: this.registerForm.controls.email.value.trim(),
        accessCode: this.registerForm.controls.accessCode.value
      });

      await this.navigateAfterAuthentication();
    } catch (error: unknown) {
      this.error.set(this.toErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  protected startGoogleAuthentication(): void {
    if (!this.googleAvailable()) {
      this.error.set('La connexion Google n’est pas encore configurée.');
      return;
    }

    const returnUrl = this.safeReturnUrl();
    this.sessionStorage()?.setItem(LoginPageComponent.GOOGLE_RETURN_URL_KEY, returnUrl);
    globalThis.location.assign('/api/auth/google/authorize/google');
  }

  protected async confirmGoogleLink(): Promise<void> {
    const code = this.googleLinkCode();
    if (code === null || this.googleLinkForm.invalid) {
      this.googleLinkForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    try {
      await this.auth.exchangeOAuthCode({
        code,
        accessCode: this.googleLinkForm.controls.accessCode.value
      });
      this.googleLinkCode.set(null);
      await this.navigateAfterAuthentication();
    } catch (error: unknown) {
      this.error.set(this.toErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private safeReturnUrl(): string {
    const value =
      this.route.snapshot.queryParamMap.get('returnUrl') ??
      this.sessionStorage()?.getItem(LoginPageComponent.GOOGLE_RETURN_URL_KEY) ??
      null;

    if (value === null || !value.startsWith('/')) {
      return this.features.defaultPath();
    }

    if (value.startsWith('/admin') && !this.auth.isAdmin()) {
      return this.features.defaultPath();
    }

    return value;
  }

  private async navigateAfterAuthentication(): Promise<void> {
    const returnUrl = this.safeReturnUrl();
    this.sessionStorage()?.removeItem(LoginPageComponent.GOOGLE_RETURN_URL_KEY);
    try {
      await this.features.ensureLoaded();
    } catch {
      this.error.set('Connexion réussie. Chargement de l’événement en cours…');
      return;
    }
    if (this.tutorial.shouldStart()) {
      await this.router.navigateByUrl('/tutorial');
      return;
    }

    await this.router.navigateByUrl(returnUrl);
  }

  private async initializeGoogleAuthentication(): Promise<void> {
    const callbackParameters = new URLSearchParams(globalThis.location.hash.replace(/^#/, ''));
    const googleStatus = callbackParameters.get('google');
    const code = callbackParameters.get('code');

    if (googleStatus !== null) {
      globalThis.history.replaceState({}, '', '/login');
    }

    if (googleStatus === 'error') {
      this.error.set('Connexion Google annulee ou impossible.');
    } else if (googleStatus === 'success' && code !== null) {
      await this.exchangeGoogleCode(code);
    }

    try {
      this.googleAvailable.set((await this.auth.providers()).google);
    } catch {
      this.googleAvailable.set(false);
    }
  }

  private async loadPartners(): Promise<void> {
    try {
      this.partners.set(await this.partnerService.findVisible());
    } catch {
      this.partners.set([]);
    }
  }

  private async exchangeGoogleCode(code: string): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      await this.auth.exchangeOAuthCode({ code });
      await this.navigateAfterAuthentication();
    } catch (error: unknown) {
      if (this.isAccountLinkRequired(error)) {
        this.googleLinkCode.set(code);
      } else {
        this.error.set(this.toErrorMessage(error));
      }
    } finally {
      this.loading.set(false);
    }
  }

  private isAccountLinkRequired(error: unknown): boolean {
    return (
      error instanceof HttpErrorResponse &&
      error.status === 409 &&
      typeof error.error === 'object' &&
      error.error !== null &&
      (error.error as Record<string, unknown>)['code'] === 'ACCOUNT_LINK_REQUIRED'
    );
  }

  private toErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      return 'Identifiants invalides.';
    }

    if (error instanceof HttpErrorResponse && error.status === 409) {
      return 'Un compte existe deja avec cet email.';
    }

    return 'Connexion impossible pour le moment.';
  }

  private sessionStorage(): Storage | null {
    return typeof globalThis.sessionStorage === 'undefined' ? null : globalThis.sessionStorage;
  }
}
