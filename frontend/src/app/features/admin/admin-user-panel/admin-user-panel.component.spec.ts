import { TestBed } from '@angular/core/testing';
import { provideTaiga } from '@taiga-ui/core';

import { AdminUserResponse, UserImportPreview } from '../models/admin-api.model';
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
    expect(fixture.nativeElement.querySelector('.import-summary').textContent).toContain(
      '1 à créer'
    );
    expect(fixture.nativeElement.querySelector('.import-table').textContent).toContain(
      'Grégory JOBLIN'
    );
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

  it('displays users ten at a time', async () => {
    adminApi.findUsers.and.resolveTo(Array.from({ length: 23 }, (_, index) => user(index + 1)));
    const fixture = TestBed.createComponent(AdminUserPanelComponent);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.pagedUsers().length).toBe(10);
    expect(fixture.componentInstance.pageCount()).toBe(3);
    expect(fixture.nativeElement.querySelectorAll('.user-table-wrap tbody tr').length).toBe(10);

    goToPage(fixture.componentInstance, 2);
    fixture.detectChanges();

    expect(fixture.componentInstance.pagedUsers().length).toBe(3);
    expect(fixture.nativeElement.querySelector('.users-pagination').textContent).toContain(
      '21–23 sur 23'
    );
  });

  it('searches instantly by first name, surname and email and resets the page', async () => {
    adminApi.findUsers.and.resolveTo([
      { ...user(1), name: 'Élodie', surname: 'Martin', email: 'elodie@example.com' },
      { ...user(2), name: 'Louis', surname: 'Bernard', email: 'contact@ecurie.fr' },
      ...Array.from({ length: 10 }, (_, index) => user(index + 3))
    ]);
    const fixture = TestBed.createComponent(AdminUserPanelComponent);
    await fixture.whenStable();
    goToPage(fixture.componentInstance, 1);

    fixture.componentInstance.searchControl.setValue('elodie martin');
    fixture.detectChanges();
    expect(fixture.componentInstance.pageIndex()).toBe(0);
    expect(fixture.componentInstance.filteredUsers().map(({ id }) => id)).toEqual([1]);

    fixture.componentInstance.searchControl.setValue('BERNARD');
    expect(fixture.componentInstance.filteredUsers().map(({ id }) => id)).toEqual([2]);

    fixture.componentInstance.searchControl.setValue('ecurie.fr');
    expect(fixture.componentInstance.filteredUsers().map(({ id }) => id)).toEqual([2]);
  });

  it('requires confirmation in a dialog before deleting a user', async () => {
    const fixture = TestBed.createComponent(AdminUserPanelComponent);
    await fixture.whenStable();
    const target = user(42);

    requestDelete(fixture.componentInstance, target);
    fixture.detectChanges();

    expect(fixture.componentInstance.deleteTarget()).toBe(target);
    expect(adminApi.deleteUser).not.toHaveBeenCalled();

    closeDeleteDialog(fixture.componentInstance);
    expect(fixture.componentInstance.deleteTarget()).toBeNull();
    expect(adminApi.deleteUser).not.toHaveBeenCalled();
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

function goToPage(component: AdminUserPanelComponent, index: number): void {
  (component as unknown as { goToPage(index: number): void }).goToPage(index);
}

function requestDelete(component: AdminUserPanelComponent, target: AdminUserResponse): void {
  (component as unknown as { requestDelete(user: AdminUserResponse): void }).requestDelete(target);
}

function closeDeleteDialog(component: AdminUserPanelComponent): void {
  (component as unknown as { onDeleteDialogChange(open: boolean): void }).onDeleteDialogChange(
    false
  );
}

function user(id: number): AdminUserResponse {
  return {
    id,
    name: `Prénom ${id}`,
    surname: `Nom ${id}`,
    birthDate: null,
    email: `user${id}@example.com`,
    present: id % 2 === 0,
    roles: ['USER']
  };
}
