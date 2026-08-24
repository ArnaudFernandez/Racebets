import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideTaiga } from '@taiga-ui/core';

import { LiveRace } from '../models/betting.model';
import { BettingApiService } from '../services/betting-api.service';
import { PartnerService } from '../services/partner.service';
import { LiveBettingBoardComponent } from './live-betting-board.component';

describe('LiveBettingBoardComponent', () => {
  it('uses the admin race hierarchy and adapts runner order to the race state', async () => {
    const liveRace = signal<LiveRace | null>(finishedRace());

    await TestBed.configureTestingModule({
      imports: [LiveBettingBoardComponent],
      providers: [
        provideTaiga(),
        {
          provide: BettingApiService,
          useValue: { liveRace, placeBet: jasmine.createSpy('placeBet') }
        },
        {
          provide: PartnerService,
          useValue: { findVisible: () => Promise.resolve([]) }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(LiveBettingBoardComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const hero = fixture.nativeElement.querySelector('.race-stage');
    const cards = [...fixture.nativeElement.querySelectorAll('.runner-card')] as HTMLElement[];
    expect(hero.querySelector('h1').textContent).toContain('Prix Carrefour');
    expect(hero.querySelector('.race-stage-image').getAttribute('src')).toBe('/logo_le_bouscat.png');
    expect(hero.textContent).not.toContain('3 partants');
    expect(hero.textContent).not.toContain('6 paris');
    expect(hero.querySelector('.stage-summary')).toBeNull();
    expect(cards.map((card) => card.querySelector('.runner-main strong')?.textContent?.trim()))
      .toEqual(['Bellino II', 'Ourasi', 'Idéal du Gazeau']);

    liveRace.update((race) => race && {...race, state: 'BETTING'});
    fixture.detectChanges();
    const bettingCards = [...fixture.nativeElement.querySelectorAll('.runner-card')] as HTMLElement[];
    expect(bettingCards.map((card) => card.querySelector('.runner-main strong')?.textContent?.trim()))
      .toEqual(['Ourasi', 'Bellino II', 'Idéal du Gazeau']);
    expect(fixture.nativeElement.querySelector('.race-stage').textContent).toContain('Les paris sont ouverts.');
  });
});

function finishedRace(): LiveRace {
  return {
    raceId: 4,
    raceName: 'Prix Carrefour',
    raceImgUrl: '/logo_le_bouscat.png',
    state: 'FINISHED',
    runners: [
      { entryId: 10, horseId: 110, horseName: 'Ourasi', horseNumber: 1, rank: 2, betCount: 2 },
      { entryId: 11, horseId: 111, horseName: 'Bellino II', horseNumber: 2, rank: 1, betCount: 3 },
      { entryId: 12, horseId: 112, horseName: 'Idéal du Gazeau', horseNumber: 3, rank: 3, betCount: 1 }
    ],
    userBet: null,
    totalBets: 6,
    updatedAt: '2026-08-24T12:00:00Z'
  };
}
