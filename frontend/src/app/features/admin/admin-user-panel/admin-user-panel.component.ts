import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TuiButton, TuiCheckbox, TuiInput, TuiTitle } from '@taiga-ui/core';
import { TuiCard, TuiHeader } from '@taiga-ui/layout';
import { TuiBadge, TuiSwitch } from '@taiga-ui/kit';

import { AdminUserRequest, AdminUserResponse, UserRole } from '../models/admin-api.model';
import { AdminApiService } from '../services/admin-api.service';

interface BackendErrorResponse {
  readonly message: string;
}

@Component({
  selector: 'app-admin-user-panel',
  imports: [ReactiveFormsModule, TuiBadge, TuiButton, TuiCard, TuiCheckbox, TuiHeader, TuiInput, TuiSwitch, TuiTitle],
  templateUrl: './admin-user-panel.component.html',
  styleUrl: './admin-user-panel.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminUserPanelComponent {
  private readonly adminApi = inject(AdminApiService);

  readonly users = signal<readonly AdminUserResponse[]>([]);
  readonly editingUserId = signal<number | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly deleteTarget = signal<AdminUserResponse | null>(null);

  readonly userForm = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(80)] }),
    surname: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(80)] }),
    birthDate: new FormControl<string | null>(null),
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email, Validators.maxLength(180)] }),
    accessCode: new FormControl<string | null>(null, [Validators.minLength(8), Validators.maxLength(128)]),
    present: new FormControl(true, { nonNullable: true }),
    admin: new FormControl(false, { nonNullable: true }),
    user: new FormControl(true, { nonNullable: true }),
    vip: new FormControl(false, { nonNullable: true })
  });

  constructor() {
    void this.refresh();
  }

  protected async refresh(): Promise<void> {
    await this.runAction(async () => {
      this.users.set(await this.adminApi.findUsers());
    }, false);
  }

  protected async submit(): Promise<void> {
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }

    const request = this.buildRequest();
    if (request === null) {
      this.error.set('Selectionnez au moins un role.');
      return;
    }

    if (this.editingUserId() === null && !request.accessCode) {
      this.error.set('Un code d acces est requis pour creer un utilisateur.');
      return;
    }

    await this.runAction(async () => {
      const id = this.editingUserId();
      if (id === null) {
        await this.adminApi.createUser(request);
        this.success.set('Utilisateur cree.');
      } else {
        await this.adminApi.updateUser(id, request);
        this.success.set('Utilisateur mis a jour.');
      }
      this.reset();
      this.users.set(await this.adminApi.findUsers());
    });
  }

  protected edit(user: AdminUserResponse): void {
    this.editingUserId.set(user.id);
    this.userForm.reset({
      name: user.name,
      surname: user.surname,
      birthDate: user.birthDate,
      email: user.email,
      accessCode: null,
      present: user.present,
      admin: user.roles.includes('ADMIN'),
      user: user.roles.includes('USER'),
      vip: user.roles.includes('VIP')
    });
    this.error.set(null);
    this.success.set(null);
  }

  protected reset(): void {
    this.editingUserId.set(null);
    this.userForm.reset({
      name: '',
      surname: '',
      birthDate: null,
      email: '',
      accessCode: null,
      present: true,
      admin: false,
      user: true,
      vip: false
    });
  }

  protected requestDelete(user: AdminUserResponse): void {
    this.deleteTarget.set(user);
  }

  protected cancelDelete(): void {
    this.deleteTarget.set(null);
  }

  protected async confirmDelete(): Promise<void> {
    const user = this.deleteTarget();
    if (user === null) {
      return;
    }

    this.deleteTarget.set(null);

    await this.runAction(async () => {
      await this.adminApi.deleteUser(user.id);
      this.users.set(await this.adminApi.findUsers());
      this.success.set('Utilisateur supprime.');
    });
  }

  private buildRequest(): AdminUserRequest | null {
    const roles: UserRole[] = [];
    if (this.userForm.controls.admin.value) roles.push('ADMIN');
    if (this.userForm.controls.user.value) roles.push('USER');
    if (this.userForm.controls.vip.value) roles.push('VIP');
    if (roles.length === 0) {
      return null;
    }

    const accessCode = this.userForm.controls.accessCode.value;
    return {
      name: this.userForm.controls.name.value.trim(),
      surname: this.userForm.controls.surname.value.trim(),
      birthDate: this.userForm.controls.birthDate.value,
      email: this.userForm.controls.email.value.trim(),
      accessCode: accessCode === null || accessCode.trim() === '' ? null : accessCode,
      present: this.userForm.controls.present.value,
      roles
    };
  }

  private async runAction(action: () => Promise<void>, clearSuccess = true): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    if (clearSuccess) this.success.set(null);
    try {
      await action();
    } catch (error: unknown) {
      this.error.set(this.toErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private toErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401 || error.status === 403) return 'Acces admin refuse.';
      if (error.status === 409 && this.isBackendError(error.error)) return error.error.message;
      if (error.status === 404) return 'Utilisateur introuvable.';
    }
    return 'Operation utilisateur impossible pour le moment.';
  }

  private isBackendError(value: unknown): value is BackendErrorResponse {
    return typeof value === 'object' && value !== null && typeof (value as Record<string, unknown>)['message'] === 'string';
  }
}
