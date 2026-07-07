import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TuiButton, TuiInput, TuiTitle } from '@taiga-ui/core';
import { TuiCard, TuiHeader } from '@taiga-ui/layout';

import { AuthService } from '../../../core/auth/auth.service';
import { AppFeaturesService } from '../../../core/features/app-features.service';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, TuiButton, TuiCard, TuiHeader, TuiInput, TuiTitle],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoginPageComponent {
  private readonly auth = inject(AuthService);
  private readonly features = inject(AppFeaturesService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly mode = signal<'login' | 'register'>('login');

  readonly form = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email]
    }),
    accessCode: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required]
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
    birthDate: new FormControl<string | null>(null),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(180)]
    }),
    accessCode: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8), Validators.maxLength(128)]
    })
  });

  protected setMode(mode: 'login' | 'register'): void {
    this.mode.set(mode);
    this.error.set(null);
  }

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    try {
      await this.auth.login({
        email: this.form.controls.email.value.trim(),
        accessCode: this.form.controls.accessCode.value
      });

      await this.router.navigateByUrl(this.safeReturnUrl());
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
        birthDate: this.registerForm.controls.birthDate.value,
        email: this.registerForm.controls.email.value.trim(),
        accessCode: this.registerForm.controls.accessCode.value
      });

      await this.router.navigateByUrl(this.safeReturnUrl());
    } catch (error: unknown) {
      this.error.set(this.toErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private safeReturnUrl(): string {
    const value = this.route.snapshot.queryParamMap.get('returnUrl');

    if (value === null || !value.startsWith('/')) {
      return this.features.defaultPath();
    }

    if (value.startsWith('/admin') && !this.auth.isAdmin()) {
      return this.features.defaultPath();
    }

    return value;
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
}
