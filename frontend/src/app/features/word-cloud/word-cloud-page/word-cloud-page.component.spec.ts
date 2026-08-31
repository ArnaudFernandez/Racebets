import { DestroyRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideTaiga } from '@taiga-ui/core';

import { WordCloudSnapshot } from '../models/word-cloud.model';
import { WordCloudApiService } from '../services/word-cloud-api.service';
import { WordCloudPageComponent } from './word-cloud-page.component';

class WordCloudApiStub {
  readonly snapshot = signal<WordCloudSnapshot | null>(null);
  readonly liveSnapshot = this.snapshot.asReadonly();
  readonly unavailable = signal(false).asReadonly();

  startPlayerPolling(destroyRef: DestroyRef): void {
    void destroyRef;
  }

  submitResponse(): Promise<WordCloudSnapshot> {
    const snapshot = this.snapshot();
    if (snapshot === null) throw new Error('A live snapshot is required');
    return Promise.resolve(snapshot);
  }
}

describe('WordCloudPageComponent', () => {
  it('shows the inline response form while open and the anonymous cloud once revealed', async () => {
    const api = new WordCloudApiStub();
    await TestBed.configureTestingModule({
      imports: [WordCloudPageComponent],
      providers: [{ provide: WordCloudApiService, useValue: api }, provideTaiga()]
    }).compileComponents();
    const fixture = TestBed.createComponent(WordCloudPageComponent);
    const openSnapshot: WordCloudSnapshot = {
      id: 4,
      text: 'Un mot pour cette course ?',
      status: 'OPEN',
      openedAt: '2026-08-31T12:00:00Z',
      submissionCount: 2,
      currentUserResponse: 'Rapide',
      words: []
    };

    api.snapshot.set(openSnapshot);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.response-dialog')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('input').value).toBe('Rapide');

    api.snapshot.set({
      ...openSnapshot,
      status: 'REVEALED',
      submissionCount: 3,
      words: [{ text: 'Énergie', count: 3 }, { text: 'Vitesse', count: 1 }]
    });
    fixture.detectChanges();
    const words = [...fixture.nativeElement.querySelectorAll('.cloud-word')] as HTMLElement[];
    expect(words.map((word) => word.textContent?.trim())).toEqual(['Énergie', 'Vitesse']);
    expect(words[0].getAttribute('aria-label')).toContain('3 occurrences');
  });
});
