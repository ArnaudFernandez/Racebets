import { TestBed } from '@angular/core/testing';
import { provideTaiga } from '@taiga-ui/core';

import { UserImportPreview } from '../models/admin-api.model';
import { AdminApiService } from '../services/admin-api.service';
import { AdminUserPanelComponent } from './admin-user-panel.component';

describe('AdminUserPanelComponent import', () => {
  let adminApi: jasmine.SpyObj<AdminApiService>;

  const preview: UserImportPreview = {
    fileDigest: 'file-digest',
    planFingerprint: 'plan-fingerprint',
    totalRows: 2,
    createCount: 1,
    updateCount: 1,
    unchangedCount: 0,
    errorCount: 0,
    importable: true,
    rows: [
      {
        lineNumber: 3,
        email: 'gregory@example.com',
        name: 'Grégory',
        surname: 'JOBLIN',
        action: 'CREATE',
        warning: null,
        error: null
      },
      {
        lineNumber: 4,
        email: 'lea@example.com',
        name: 'Léa',
        surname: 'PEYRAN',
        action: 'UPDATE',
        warning: 'Colonnes supplémentaires ignorées.',
        error: null
      }
    ]
  };

  beforeEach(async () => {
    adminApi = jasmine.createSpyObj<AdminApiService>('AdminApiService', [
      'findUsers',
      'createUser',
      'updateUser',
      'deleteUser',
      'previewUserImport',
      'confirmUserImport'
    ]);
    adminApi.findUsers.and.resolveTo([]);
    adminApi.previewUserImport.and.resolveTo(preview);
    adminApi.confirmUserImport.and.resolveTo({
      createdCount: 1,
      updatedCount: 1,
      unchangedCount: 0
    });

    await TestBed.configureTestingModule({
      imports: [AdminUserPanelComponent],
      providers: [provideTaiga(), { provide: AdminApiService, useValue: adminApi }]
    }).compileComponents();
  });

  it('previews decoded participants before importing', async () => {
    const fixture = TestBed.createComponent(AdminUserPanelComponent);
    await fixture.whenStable();
    const component = fixture.componentInstance;
    const file = new File(['csv'], 'participants.csv', { type: 'text/csv' });
    component.importFile.set(file);

    await analyzeImport(component);
    fixture.detectChanges();

    expect(adminApi.previewUserImport).toHaveBeenCalledOnceWith(file);
    expect(fixture.nativeElement.querySelector('.import-summary').textContent).toContain('1 à créer');
    expect(fixture.nativeElement.querySelector('.import-table').textContent).toContain('Grégory JOBLIN');
    expect(adminApi.confirmUserImport).not.toHaveBeenCalled();
  });

  it('imports only after confirmation and refreshes the user list', async () => {
    const fixture = TestBed.createComponent(AdminUserPanelComponent);
    await fixture.whenStable();
    const component = fixture.componentInstance;
    const file = new File(['csv'], 'participants.csv', { type: 'text/csv' });
    component.importFile.set(file);
    component.importPreview.set(preview);

    requestImport(component);
    expect(component.importConfirmationOpen()).toBeTrue();
    expect(adminApi.confirmUserImport).not.toHaveBeenCalled();

    await confirmImport(component);

    expect(adminApi.confirmUserImport).toHaveBeenCalledOnceWith(file, preview);
    expect(component.importPreview()).toBeNull();
    expect(component.success()).toContain('1 compte(s) créé(s), 1 mis à jour');
    expect(adminApi.findUsers).toHaveBeenCalledTimes(2);
  });
});

function analyzeImport(component: AdminUserPanelComponent): Promise<void> {
  return (component as unknown as { analyzeImport(): Promise<void> }).analyzeImport();
}

function requestImport(component: AdminUserPanelComponent): void {
  (component as unknown as { requestImport(): void }).requestImport();
}

function confirmImport(component: AdminUserPanelComponent): Promise<void> {
  return (component as unknown as { confirmImport(): Promise<void> }).confirmImport();
}
