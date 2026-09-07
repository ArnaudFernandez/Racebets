import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TuiTable } from '@taiga-ui/addon-table';
import {
  TuiButton,
  TuiCell,
  TuiCheckbox,
  TuiDialog,
  TuiInput,
  TuiLabel,
  TuiLoader,
  TuiTitle
} from '@taiga-ui/core';
import { TuiCard, TuiHeader } from '@taiga-ui/layout';
import {
  TuiAutoColorPipe,
  TuiAvatar,
  TuiBadge,
  TuiInitialsPipe,
  TuiPagination,
  TuiSwitch
} from '@taiga-ui/kit';

import {
  AdminUserRequest,
  AdminUserResponse,
  UserImportAction,
  UserImportPreview,
  UserRole
} from '../models/admin-api.model';
import { AdminApiService } from '../services/admin-api.service';

interface BackendErrorResponse {
  readonly message: string;
}

const USER_PAGE_SIZE = 10;

@Component({
  selector: 'app-admin-user-panel',
  imports: [
    ReactiveFormsModule,
    TuiAutoColorPipe,
    TuiAvatar,
    TuiBadge,
    TuiButton,
    TuiCard,
    TuiCell,
    TuiCheckbox,
    TuiDialog,
    TuiHeader,
    TuiInitialsPipe,
    TuiInput,
    TuiLabel,
    TuiLoader,
    TuiPagination,
    TuiSwitch,
    TuiTable,
    TuiTitle
  ],
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
  readonly importFile = signal<File | null>(null);
  readonly importPreview = signal<UserImportPreview | null>(null);
  readonly importConfirmationOpen = signal(false);
  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly searchQuery = toSignal(this.searchControl.valueChanges, { initialValue: '' });
  readonly pageIndex = signal(0);
  readonly filteredUsers = computed(() => {
    const terms = this.normalizeSearch(this.searchQuery()).split(/\s+/).filter(Boolean);
    if (terms.length === 0) return this.users();

    return this.users().filter((user) => {
      const searchableText = this.normalizeSearch(`${user.name} ${user.surname} ${user.email}`);
      return terms.every((term) => searchableText.includes(term));
    });
  });
  readonly pageCount = computed(() => Math.ceil(this.filteredUsers().length / USER_PAGE_SIZE));
  readonly pagedUsers = computed(() => {
    const start = this.pageIndex() * USER_PAGE_SIZE;
    return this.filteredUsers().slice(start, start + USER_PAGE_SIZE);
  });
  readonly firstVisibleUser = computed(() =>
    this.filteredUsers().length === 0 ? 0 : this.pageIndex() * USER_PAGE_SIZE + 1
  );
  readonly lastVisibleUser = computed(() =>
    Math.min((this.pageIndex() + 1) * USER_PAGE_SIZE, this.filteredUsers().length)
  );

  readonly userForm = new FormGroup({
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
    accessCode: new FormControl<string | null>(null, [
      Validators.minLength(8),
      Validators.maxLength(128)
    ]),
    present: new FormControl(true, { nonNullable: true }),
    admin: new FormControl(false, { nonNullable: true }),
    user: new FormControl(true, { nonNullable: true }),
    vip: new FormControl(false, { nonNullable: true })
  });

  constructor() {
    this.searchControl.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.pageIndex.set(0));
    void this.refresh();
  }

  protected async refresh(): Promise<void> {
    await this.runAction(async () => {
      this.setUsers(await this.adminApi.findUsers());
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
      this.setUsers(await this.adminApi.findUsers());
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

  protected onDeleteDialogChange(open: boolean): void {
    if (!open) this.cancelDelete();
  }

  protected async confirmDelete(): Promise<void> {
    const user = this.deleteTarget();
    if (user === null) {
      return;
    }

    this.deleteTarget.set(null);

    await this.runAction(async () => {
      await this.adminApi.deleteUser(user.id);
      this.setUsers(await this.adminApi.findUsers());
      this.success.set('Utilisateur supprime.');
    });
  }

  protected selectImportFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0) ?? null;
    this.importPreview.set(null);
    this.importConfirmationOpen.set(false);
    if (file !== null && (!file.name.toLowerCase().endsWith('.csv') || file.size > 1024 * 1024)) {
      this.importFile.set(null);
      this.error.set('Choisissez un fichier CSV de 1 Mo maximum.');
      input.value = '';
      return;
    }
    this.importFile.set(file);
    this.error.set(null);
    this.success.set(null);
  }

  protected async analyzeImport(): Promise<void> {
    const file = this.importFile();
    if (file === null) {
      this.error.set('Sélectionnez un fichier CSV à analyser.');
      return;
    }
    await this.runAction(async () => {
      this.importPreview.set(await this.adminApi.previewUserImport(file));
    });
  }

  protected requestImport(): void {
    if (this.importPreview()?.importable) this.importConfirmationOpen.set(true);
  }

  protected cancelImport(): void {
    this.importConfirmationOpen.set(false);
  }

  protected async confirmImport(): Promise<void> {
    const file = this.importFile();
    const preview = this.importPreview();
    if (file === null || preview === null || !preview.importable) {
      this.importConfirmationOpen.set(false);
      return;
    }
    this.importConfirmationOpen.set(false);
    await this.runAction(async () => {
      const result = await this.adminApi.confirmUserImport(file, preview);
      this.setUsers(await this.adminApi.findUsers());
      this.importFile.set(null);
      this.importPreview.set(null);
      this.success.set(
        `${result.createdCount} compte(s) créé(s), ${result.updatedCount} mis à jour, ${result.unchangedCount} inchangé(s).`
      );
    });
  }

  protected importActionLabel(action: UserImportAction): string {
    return {
      CREATE: 'À créer',
      UPDATE: 'À mettre à jour',
      UNCHANGED: 'Inchangé',
      ERROR: 'Erreur'
    }[action];
  }

  protected goToPage(index: number): void {
    this.pageIndex.set(index);
  }

  protected displayName(user: AdminUserResponse): string {
    return `${user.name} ${user.surname}`;
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

  private setUsers(users: readonly AdminUserResponse[]): void {
    this.users.set(users);
    this.pageIndex.set(0);
  }

  private normalizeSearch(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLocaleLowerCase('fr')
      .trim();
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
      if ((error.status === 400 || error.status === 409) && this.isBackendError(error.error))
        return error.error.message;
      if (error.status === 404) return 'Utilisateur introuvable.';
    }
    return 'Operation utilisateur impossible pour le moment.';
  }

  private isBackendError(value: unknown): value is BackendErrorResponse {
    return (
      typeof value === 'object' &&
      value !== null &&
      typeof (value as Record<string, unknown>)['message'] === 'string'
    );
  }
}
