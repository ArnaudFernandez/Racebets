import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, discardPeriodicTasks, fakeAsync, tick } from '@angular/core/testing';

import { WordCloudSnapshot } from '../models/word-cloud.model';
import { WordCloudApiService } from '../services/word-cloud-api.service';
import { PublicWordCloudPageComponent } from './public-word-cloud-page.component';

describe('PublicWordCloudPageComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PublicWordCloudPageComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
  });

  it('only displays the waiting message when there is no active word cloud', fakeAsync(() => {
    const fixture = TestBed.createComponent(PublicWordCloudPageComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    tick();

    http
      .expectOne('/api/word-cloud/public/live')
      .flush(null, { status: 204, statusText: 'No Content' });
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent.trim()).toBe("En attente d'un nuage de mots");
    expect(fixture.nativeElement.querySelector('.cloud-field')).toBeNull();

    fixture.destroy();
    http.verify();
    discardPeriodicTasks();
  }));

  it('updates the visible cloud during collection and keeps it visible after reveal', fakeAsync(() => {
    const fixture = TestBed.createComponent(PublicWordCloudPageComponent);
    const http = TestBed.inject(HttpTestingController);
    const api = TestBed.inject(WordCloudApiService);
    const openSnapshot: WordCloudSnapshot = {
      id: 8,
      text: 'Quel mot résume cette course ?',
      status: 'OPEN',
      openedAt: '2026-09-06T12:00:00Z',
      submissionCount: 1,
      currentUserResponse: null,
      words: [{ text: 'Vitesse', count: 1 }]
    };
    fixture.detectChanges();
    tick();

    http.expectOne('/api/word-cloud/public/live').flush(openSnapshot);
    tick();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('h1').textContent).toContain(openSnapshot.text);
    expect(visibleWords(fixture.nativeElement)).toEqual(['Vitesse']);
    expect(api.liveSnapshot()).toBeNull();

    tick(1000);
    http.expectOne('/api/word-cloud/public/live').flush({
      ...openSnapshot,
      submissionCount: 3,
      words: [
        { text: 'Vitesse', count: 2 },
        { text: 'Énergie', count: 1 }
      ]
    });
    tick();
    fixture.detectChanges();
    expect(visibleWords(fixture.nativeElement)).toEqual(['Vitesse', 'Énergie']);
    expect(fixture.nativeElement.querySelector('.cloud-word').getAttribute('aria-label')).toContain(
      '2 occurrences'
    );

    tick(1000);
    http.expectOne('/api/word-cloud/public/live').flush({ ...openSnapshot, status: 'REVEALED' });
    tick();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.cloud-field')).not.toBeNull();
    expect(visibleWords(fixture.nativeElement)).toEqual(['Vitesse']);

    fixture.destroy();
    http.verify();
    discardPeriodicTasks();
  }));
});

function visibleWords(element: HTMLElement): string[] {
  return [...element.querySelectorAll<HTMLElement>('.cloud-word')].map(
    (word) => word.textContent?.trim() ?? ''
  );
}
