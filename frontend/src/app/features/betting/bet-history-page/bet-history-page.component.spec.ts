import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, discardPeriodicTasks, fakeAsync, tick } from '@angular/core/testing';
import { provideTaiga } from '@taiga-ui/core';

import { BetHistoryEntry } from '../models/bet-history.model';
import { BetHistoryPageComponent } from './bet-history-page.component';

describe('BetHistoryPageComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BetHistoryPageComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTaiga()]
    }).compileComponents();
  });

  it('updates a player result automatically after an admin correction', fakeAsync(() => {
    const fixture = TestBed.createComponent(BetHistoryPageComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    tick();

    http.expectOne('/api/betting/history').flush([historyEntry('LOST', 'Ourasi', null)]);
    tick();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Perdu');
    expect(fixture.nativeElement.textContent).toContain('Ourasi');

    tick(1000);
    http.expectOne('/api/betting/history').flush([historyEntry('WON', 'Bellino II', 1)]);
    tick();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Gagné');
    expect(fixture.nativeElement.textContent).toContain('Bellino II');
    expect(fixture.nativeElement.textContent).toContain('#1');

    fixture.destroy();
    http.verify();
    discardPeriodicTasks();
  }));
});

function historyEntry(
  state: BetHistoryEntry['state'],
  winningHorseName: string,
  speedRank: number | null
): BetHistoryEntry {
  return {
    raceId: 4,
    raceName: 'Prix Carrefour',
    finishedAt: '2026-08-24T09:00:00Z',
    selectedHorseName: 'Bellino II',
    winningHorseName,
    placedAt: '2026-08-24T08:59:30Z',
    state,
    speedRank
  };
}
