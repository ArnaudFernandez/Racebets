import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { provideTaiga } from '@taiga-ui/core';

import { RaceHistoryDetail } from '../models/admin-api.model';
import { AdminApiService } from '../services/admin-api.service';
import { RaceHistoryDetailPageComponent } from './race-history-detail-page.component';

describe('RaceHistoryDetailPageComponent', () => {
  let adminApi: jasmine.SpyObj<AdminApiService>;

  beforeEach(async () => {
    adminApi = jasmine.createSpyObj<AdminApiService>('AdminApiService', [
      'getRaceHistoryDetail',
      'correctRaceResult'
    ]);
    adminApi.getRaceHistoryDetail.and.resolveTo(history());

    await TestBed.configureTestingModule({
      imports: [RaceHistoryDetailPageComponent],
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

  it('restores the official order when result edition is cancelled', async () => {
    const fixture = TestBed.createComponent(RaceHistoryDetailPageComponent);
    await fixture.whenStable();
    const component = fixture.componentInstance;
    const actions = component as unknown as {
      editResult(): void;
      reorderEntries(order: readonly number[]): void;
      cancelResultEdition(): void;
    };

    actions.editResult();
    actions.reorderEntries([11, 10]);
    expect(component.arrivalOrder()).toEqual([11, 10]);
    expect(component.resultChanged()).toBeTrue();

    actions.cancelResultEdition();
    expect(component.arrivalOrder()).toEqual([10, 11]);
    expect(component.resultChanged()).toBeFalse();
  });

  it('persists the complete corrected order and refreshes derived results', async () => {
    const corrected = history([11, 10]);
    adminApi.correctRaceResult.and.resolveTo(corrected);
    const fixture = TestBed.createComponent(RaceHistoryDetailPageComponent);
    await fixture.whenStable();
    const component = fixture.componentInstance;
    const actions = component as unknown as {
      editResult(): void;
      reorderEntries(order: readonly number[]): void;
      requestResultCorrection(): void;
      confirmResultCorrection(): Promise<void>;
    };

    actions.editResult();
    actions.reorderEntries([11, 10]);
    actions.requestResultCorrection();
    expect(component.correctionDialogOpen()).toBeTrue();

    await actions.confirmResultCorrection();

    expect(adminApi.correctRaceResult).toHaveBeenCalledOnceWith(4, [10, 11], [11, 10]);
    expect(component.history()).toBe(corrected);
    expect(component.editingResult()).toBeFalse();
    expect(component.success()).toContain('corrigés');
  });

  it('reloads the official result after a concurrent correction conflict', async () => {
    const latest = history([11, 10]);
    adminApi.correctRaceResult.and.rejectWith(
      new HttpErrorResponse({ status: 409, error: { message: 'Result changed' } })
    );
    adminApi.getRaceHistoryDetail.and.returnValues(Promise.resolve(history()), Promise.resolve(latest));
    const fixture = TestBed.createComponent(RaceHistoryDetailPageComponent);
    await fixture.whenStable();
    const component = fixture.componentInstance;
    const actions = component as unknown as {
      editResult(): void;
      reorderEntries(order: readonly number[]): void;
      confirmResultCorrection(): Promise<void>;
    };

    actions.editResult();
    actions.reorderEntries([11, 10]);
    await actions.confirmResultCorrection();

    expect(component.history()).toBe(latest);
    expect(component.editingResult()).toBeFalse();
    expect(component.error()).toContain('rechargé');
  });
});

function history(order: readonly number[] = [10, 11]): RaceHistoryDetail {
  const entries = {
    10: {
      entryId: 10,
      rank: order.indexOf(10) + 1,
      horseNumber: 4,
      horseName: 'Ourasi',
      voteCount: 1
    },
    11: {
      entryId: 11,
      rank: order.indexOf(11) + 1,
      horseNumber: 7,
      horseName: 'Bellino II',
      voteCount: 1
    }
  } as const;

  return {
    raceId: 4,
    raceName: 'Prix Carrefour',
    finishedAt: '2026-07-28T09:00:00Z',
    totalVotes: 2,
    result: order.map((id) => entries[id as keyof typeof entries]),
    winners: [],
    votes: []
  };
}
