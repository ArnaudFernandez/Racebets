import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideTaiga } from '@taiga-ui/core';

import { WordCloudAdminSnapshot } from '../models/word-cloud.model';
import { WordCloudApiService } from '../services/word-cloud-api.service';
import { AdminWordCloudControlPageComponent } from './admin-word-cloud-control-page.component';

const SNAPSHOT: WordCloudAdminSnapshot = {
  id: 7,
  text: 'Votre mot ?',
  status: 'OPEN',
  createdAt: '2026-08-31T11:00:00Z',
  openedAt: '2026-08-31T12:00:00Z',
  revealedAt: null,
  closedAt: null,
  submissionCount: 7,
  responses: [
    { text: 'Passion', count: 4, censored: false },
    { text: 'Victoire', count: 2, censored: false },
    { text: 'Interdit', count: 1, censored: true }
  ]
};

class WordCloudControlApiStub {
  findAdminQuestion(): Promise<WordCloudAdminSnapshot> {
    return Promise.resolve(SNAPSHOT);
  }
}

describe('AdminWordCloudControlPageComponent', () => {
  it('shows live responses in frequency order and preserves censored rows', async () => {
    await TestBed.configureTestingModule({
      imports: [AdminWordCloudControlPageComponent],
      providers: [
        { provide: WordCloudApiService, useClass: WordCloudControlApiStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '7' } } } },
        provideRouter([]),
        provideTaiga()
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(AdminWordCloudControlPageComponent);
    fixture.componentInstance.question.set(SNAPSHOT);
    fixture.componentInstance.loading.set(false);
    fixture.detectChanges();

    const rows = [...fixture.nativeElement.querySelectorAll('tbody tr')] as HTMLTableRowElement[];
    expect(rows[0].textContent).toContain('Passion');
    expect(rows[1].textContent).toContain('Victoire');
    expect(rows[2].textContent).toContain('Interdit');
    expect(rows[2].classList).toContain('censored-row');
    expect((rows[2].querySelector('button') as HTMLButtonElement).disabled).toBeTrue();

    fixture.destroy();
  });
});
