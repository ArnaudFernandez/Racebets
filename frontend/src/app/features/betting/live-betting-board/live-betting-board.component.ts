import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  OnInit,
  OnDestroy,
  output,
  untracked,
  viewChild
} from '@angular/core';
import { TuiButton } from '@taiga-ui/core';

import { SseService } from '../../../core/realtime/sse.service';
import { RealtimeCardComponent } from '../../../shared/ui';
import { RaceSummary } from '../../races/models/race.model';
import { HorseBetSelection, RaceRunner } from '../models/betting.model';
import { BettingSignalStore } from '../state/betting.signal-store';

@Component({
  selector: 'app-live-betting-board',
  imports: [
    RealtimeCardComponent,
    TuiButton
  ],
  templateUrl: './live-betting-board.component.html',
  styleUrl: './live-betting-board.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LiveBettingBoardComponent implements OnInit, OnDestroy {
  private readonly store = inject(BettingSignalStore);
  private readonly sse = inject(SseService);

  readonly race = input<RaceSummary>({
    id: 'paris-fictifs-main',
    name: 'Hippodrome Paris Fictifs',
    track: 'Paris',
    startsAt: new Date().toISOString()
  });

  readonly runnerSelected = output<HorseBetSelection>();
  readonly board = viewChild<RealtimeCardComponent>('board');

  readonly runners = computed<readonly RaceRunner[]>(() => {
    const race = this.race();
    const runnersByRace = this.store.runnersByRace();

    return runnersByRace[race.id] ?? Object.values(runnersByRace)[0] ?? [];
  });

  readonly activeRaceId = computed(() => {
    const race = this.race();
    const runnersByRace = this.store.runnersByRace();

    return runnersByRace[race.id] !== undefined ? race.id : Object.keys(runnersByRace)[0] ?? race.id;
  });

  readonly userBet = computed(() => this.store.userBet());

  constructor() {
    effect(() => {
      const updates = this.sse.bettingUpdates();
      untracked(() => {
        for (const update of updates) {
          this.store.applyBettingUpdate(update);
        }
      });
    });
  }

  ngOnInit(): void {
    this.sse.connect();
  }

  ngOnDestroy(): void {
    this.sse.disconnect();
  }

  protected selectRunner(runner: RaceRunner): void {
    const selection: HorseBetSelection = {
      raceId: this.activeRaceId(),
      runnerId: runner.runnerId,
      runnerName: runner.runnerName,
      placedAt: new Date().toISOString()
    };

    this.store.placeBet(selection);
    this.runnerSelected.emit(selection);
  }

  protected clearBet(): void {
    this.store.clearBet();
  }

  protected formatTime(value: string): string {
    return new Intl.DateTimeFormat('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    }).format(new Date(value));
  }
}
