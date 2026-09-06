import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { UserImportPreview } from '../models/admin-api.model';
import { AdminApiService } from './admin-api.service';

describe('AdminApiService user import', () => {
  let service: AdminApiService;
  let http: HttpTestingController;

  const preview: UserImportPreview = {
    fileDigest: 'file-digest',
    planFingerprint: 'plan-fingerprint',
    totalRows: 1,
    createCount: 1,
    updateCount: 0,
    unchangedCount: 0,
    errorCount: 0,
    importable: true,
    rows: []
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AdminApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uploads the CSV for preview', async () => {
    const file = new File(['Email professionnel;Prénom;Nom'], 'participants.csv');
    const loading = service.previewUserImport(file);
    const request = http.expectOne('/api/admin/users/import/preview');

    expect(request.request.method).toBe('POST');
    expect(request.request.body.get('file')).toBe(file);
    request.flush(preview);

    await expectAsync(loading).toBeResolvedTo(preview);
  });

  it('sends both preview fingerprints when confirming', async () => {
    const file = new File(['Email professionnel;Prénom;Nom'], 'participants.csv');
    const loading = service.confirmUserImport(file, preview);
    const request = http.expectOne('/api/admin/users/import/confirm');

    expect(request.request.method).toBe('POST');
    expect(request.request.body.get('file')).toBe(file);
    expect(request.request.body.get('fileDigest')).toBe('file-digest');
    expect(request.request.body.get('planFingerprint')).toBe('plan-fingerprint');
    request.flush({ createdCount: 1, updatedCount: 0, unchangedCount: 0 });

    await expectAsync(loading).toBeResolved();
  });
});
