import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';

import {
  HorseBetSelection,
  RaceBettingUpdate,
  RaceRunner,
  UserRaceBet
} from '../models/betting.model';

interface BettingState {
  readonly runnersByRace: Record<string, readonly RaceRunner[]>;
  readonly userBet: UserRaceBet;
}

const initialState: BettingState = {
  runnersByRace: {},
  userBet: {
    selection: null
  }
};

export const BettingSignalStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withMethods((store) => ({
    applyBettingUpdate(update: RaceBettingUpdate): void {
      patchState(store, {
        runnersByRace: {
          ...store.runnersByRace(),
          [update.raceId]: update.runners
        }
      });
    },
    placeBet(selection: HorseBetSelection): void {
      patchState(store, {
        userBet: {
          selection
        }
      });
    },
    clearBet(): void {
      patchState(store, {
        userBet: initialState.userBet
      });
    }
  }))
);
