import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TuiButton, TuiInput, TuiLoader } from '@taiga-ui/core';
import { TuiSwitch } from '@taiga-ui/kit';

import { PartnerAdminResponse } from '../models/admin-api.model';
import { AdminApiService } from '../services/admin-api.service';

@Component({
  selector: 'app-admin-partner-panel',
  imports: [ReactiveFormsModule, TuiButton, TuiInput, TuiLoader, TuiSwitch],
  templateUrl: './admin-partner-panel.component.html',
  styleUrl: './admin-partner-panel.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminPartnerPanelComponent {
  private readonly adminApi = inject(AdminApiService);

  readonly partners = signal<readonly PartnerAdminResponse[]>([]);
  readonly sortedPartners = computed(() => [...this.partners()].sort((left, right) => left.id - right.id));
  readonly editingId = signal<number | null>(null);
  readonly selectedLogo = signal<File | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly deleteTarget = signal<PartnerAdminResponse | null>(null);

  readonly form = new FormGroup({
    name: new FormControl('', {nonNullable: true, validators: [Validators.required, Validators.maxLength(120)]}),
    displayOnWaiting: new FormControl(true, {nonNullable: true})
  });

  constructor() {
    void this.load();
  }

  protected selectLogo(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedLogo.set(input.files?.item(0) ?? null);
  }

  protected edit(partner: PartnerAdminResponse): void {
    this.editingId.set(partner.id);
    this.selectedLogo.set(null);
    this.form.setValue({name: partner.name, displayOnWaiting: partner.displayOnWaiting});
    this.clearMessages();
  }

  protected reset(): void {
    this.editingId.set(null);
    this.selectedLogo.set(null);
    this.deleteTarget.set(null);
    this.form.reset({name: '', displayOnWaiting: true});
  }

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.editingId() === null && this.selectedLogo() === null) {
      this.error.set('Sélectionnez un logo pour créer le partenaire.');
      return;
    }

    await this.run(async () => {
      const data = new FormData();
      data.append('name', this.form.controls.name.value.trim());
      data.append('displayOnWaiting', String(this.form.controls.displayOnWaiting.value));
      const logo = this.selectedLogo();
      if (logo !== null) data.append('logo', logo);

      const id = this.editingId();
      if (id === null) {
        await this.adminApi.createPartner(data);
        this.success.set('Partenaire créé.');
      } else {
        await this.adminApi.updatePartner(id, data);
        this.success.set('Partenaire mis à jour.');
      }
      this.resetFormOnly();
      this.partners.set(await this.adminApi.findPartners());
    });
  }

  protected async confirmDelete(): Promise<void> {
    const target = this.deleteTarget();
    if (target === null) return;
    await this.run(async () => {
      await this.adminApi.deletePartner(target.id);
      this.deleteTarget.set(null);
      this.partners.set(await this.adminApi.findPartners());
      this.success.set('Partenaire supprimé.');
    });
  }

  private async load(): Promise<void> {
    await this.run(async () => this.partners.set(await this.adminApi.findPartners()));
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.loading.set(true);
    this.clearMessages();
    try {
      await action();
    } catch (error: unknown) {
      this.error.set(error instanceof HttpErrorResponse && typeof error.error?.message === 'string'
        ? error.error.message
        : 'Opération impossible pour le moment.');
    } finally {
      this.loading.set(false);
    }
  }

  private resetFormOnly(): void {
    this.editingId.set(null);
    this.selectedLogo.set(null);
    this.form.reset({name: '', displayOnWaiting: true});
  }

  private clearMessages(): void {
    this.error.set(null);
    this.success.set(null);
  }
}
