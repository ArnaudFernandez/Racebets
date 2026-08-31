import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { WordCloudSnapshot } from '../models/word-cloud.model';
import { WordCloudApiService } from './word-cloud-api.service';

describe('WordCloudApiService', () => {
  it('does not poll globally and immediately exposes a submitted server snapshot', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(WordCloudApiService);
    const http = TestBed.inject(HttpTestingController);
    const snapshot: WordCloudSnapshot = {
      id: 7,
      text: 'Un mot pour la course ?',
      status: 'OPEN',
      openedAt: '2026-08-31T12:00:00Z',
      submissionCount: 4,
      currentUserResponse: 'Énergie',
      words: []
    };

    http.expectNone('/api/word-cloud/live');
    const pending = service.submitResponse(7, 'Énergie');
    const request = http.expectOne('/api/word-cloud/questions/7/responses');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ text: 'Énergie' });
    request.flush(snapshot);

    await expectAsync(pending).toBeResolvedTo(snapshot);
    expect(service.liveSnapshot()).toEqual(snapshot);
    http.verify();
  });
});
