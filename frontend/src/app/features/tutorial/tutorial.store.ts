import { computed } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

import { TUTORIAL_STEPS, TUTORIAL_WINNER_ID } from './tutorial.fixture';

interface TutorialState {
  readonly stepIndex: number;
  readonly selectedEntryId: number | null;
}

const initialState: TutorialState = {
  stepIndex: 0,
  selectedEntryId: null
};

export const TutorialStore = signalStore(
  withState(initialState),
  withComputed((store) => ({
    step: computed(() => TUTORIAL_STEPS[store.stepIndex()]!),
    progress: computed(() => store.stepIndex() + 1)
  })),
  withMethods((store) => ({
    next(): void {
      patchState(store, (state) => ({
        stepIndex: Math.min(state.stepIndex + 1, TUTORIAL_STEPS.length - 1)
      }));
    },
    chooseWinner(entryId: number): void {
      if (store.step().action !== 'choose-runner' || entryId !== TUTORIAL_WINNER_ID) {
        return;
      }

      patchState(store, (state) => ({ selectedEntryId: entryId, stepIndex: state.stepIndex + 1 }));
    }
  }))
);
