import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { provideTaiga } from '@taiga-ui/core';

import { RaceControl, RaceHistoryDetail } from '../models/admin-api.model';
import { AdminApiService } from '../services/admin-api.service';
import { RaceControlPageComponent } from './race-control-page.component';

describe('RaceControlPageComponent', () => {
  let adminApi: jasmine.SpyObj<AdminApiService>;

  beforeEach(async () => {
    adminApi = jasmine.createSpyObj<AdminApiService>('AdminApiService', [
      'getRaceControl',
      'findHorses',
      'correctRaceResult'
    ]);
    adminApi.getRaceControl.and.resolveTo(race());
    adminApi.findHorses.and.resolveTo([]);

    await TestBed.configureTestingModule({
      imports: [RaceControlPageComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        provideTaiga(),
        { provide: AdminApiService, useValue: adminApi },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '4' } } }
        }
      ]
    }).compileComponents();
  });

  it('corrects a finished result from the pilot page and updates displayed ranks', async () => {
    adminApi.correctRaceResult.and.resolveTo(history([11, 10]));
    const fixture = TestBed.createComponent(RaceControlPageComponent);
    await initialLoad();
    const component = fixture.componentInstance;
    const actions = component as unknown as {
      editResult(): void;
      reorderEntries(order: readonly number[]): void;
      confirmResultCorrection(): Promise<void>;
    };

    actions.editResult();
    actions.reorderEntries([11, 10]);
    await actions.confirmResultCorrection();

    expect(adminApi.correctRaceResult).toHaveBeenCalledOnceWith(4, [10, 11], [11, 10]);
    expect(component.arrivalOrder()).toEqual([11, 10]);
    expect(component.race()?.entries.find((entry) => entry.entryId === 11)?.rank).toBe(1);
    expect(component.editingResult()).toBeFalse();
    expect(component.success()).toContain('joueurs');
    fixture.destroy();
  });

  it('reloads the official result when another administrator corrected it first', async () => {
    const latest = race([11, 10]);
    adminApi.correctRaceResult.and.rejectWith(new HttpErrorResponse({ status: 409 }));
    adminApi.getRaceControl.and.returnValues(Promise.resolve(race()), Promise.resolve(latest));
    const fixture = TestBed.createComponent(RaceControlPageComponent);
    await initialLoad();
    const component = fixture.componentInstance;
    const actions = component as unknown as {
      editResult(): void;
      reorderEntries(order: readonly number[]): void;
      confirmResultCorrection(): Promise<void>;
    };

    actions.editResult();
    actions.reorderEntries([11, 10]);
    await actions.confirmResultCorrection();

    expect(component.race()).toBe(latest);
    expect(component.arrivalOrder()).toEqual([11, 10]);
    expect(component.editingResult()).toBeFalse();
    expect(component.error()).toContain('rechargé');
    fixture.destroy();
  });
});

async function initialLoad(): Promise<void> {
  await Promise.resolve();
  await Promise.resolve();
}

function race(order: readonly number[] = [10, 11]): RaceControl {
  const entries = {
    10: { entryId: 10, horseId: 110, horseName: 'Ourasi', horseNumber: 4, rank: order.indexOf(10) + 1, betCount: 2 },
    11: { entryId: 11, horseId: 111, horseName: 'Bellino II', horseNumber: 7, rank: order.indexOf(11) + 1, betCount: 3 }
  } as const;
  return {
    id: 4,
    name: 'Prix Carrefour',
    raceImgUrl: null,
    state: 'FINISHED',
    visibleOnLive: false,
    entries: order.map((id) => entries[id as keyof typeof entries]),
    totalBets: 5,
    updatedAt: '2026-08-24T09:00:00Z'
  };
}

function history(order: readonly number[]): RaceHistoryDetail {
  const controlledRace = race(order);
  return {
    raceId: controlledRace.id,
    raceName: controlledRace.name,
    finishedAt: controlledRace.updatedAt,
    totalVotes: controlledRace.totalBets,
    result: controlledRace.entries.map((entry) => ({
      entryId: entry.entryId,
      rank: entry.rank!,
      horseNumber: entry.horseNumber,
      horseName: entry.horseName,
      voteCount: entry.betCount
    })),
    winners: [],
    votes: []
  };
}
