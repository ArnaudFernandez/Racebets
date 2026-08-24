import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideTaiga } from '@taiga-ui/core';

import { TutorialService } from '../../../core/tutorial/tutorial.service';
import { TUTORIAL_WINNER_ID } from '../tutorial.fixture';
import { TutorialPageComponent } from './tutorial-page.component';

describe('TutorialPageComponent', () => {
  let tutorial: jasmine.SpyObj<TutorialService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    tutorial = jasmine.createSpyObj<TutorialService>('TutorialService', ['complete']);
    tutorial.complete.and.resolveTo();
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    router.navigateByUrl.and.resolveTo(true);

    await TestBed.configureTestingModule({
      imports: [TutorialPageComponent],
      providers: [
        provideTaiga(),
        { provide: TutorialService, useValue: tutorial },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();
  });

  it('requires the highlighted horse before confirming the simulated bet', () => {
    const fixture = TestBed.createComponent(TutorialPageComponent);
    const component = fixture.componentInstance as unknown as TutorialActions;
    component.store.next();
    component.store.next();
    component.store.next();

    component.store.chooseWinner(101);
    expect(component.store.step().id).toBe('choose');

    component.store.chooseWinner(TUTORIAL_WINNER_ID);
    expect(component.store.step().id).toBe('bet-placed');
    expect(component.store.selectedEntryId()).toBe(TUTORIAL_WINNER_ID);
  });

  it('marks the tutorial complete and returns home after the history step', async () => {
    const fixture = TestBed.createComponent(TutorialPageComponent);
    const component = fixture.componentInstance as unknown as TutorialActions;
    component.store.next();
    component.store.next();
    component.store.next();
    component.store.chooseWinner(TUTORIAL_WINNER_ID);
    component.store.next();
    component.store.next();
    component.store.next();

    component.continue();
    await Promise.resolve();

    expect(tutorial.complete).toHaveBeenCalledOnceWith();
    expect(router.navigateByUrl).toHaveBeenCalledOnceWith('/');
  });

  it('stays in the tutorial when completion cannot be persisted', async () => {
    tutorial.complete.and.rejectWith(new Error('offline'));
    const fixture = TestBed.createComponent(TutorialPageComponent);
    const component = fixture.componentInstance as unknown as TutorialActions;
    component.store.next();
    component.store.next();
    component.store.next();
    component.store.chooseWinner(TUTORIAL_WINNER_ID);
    component.store.next();
    component.store.next();
    component.store.next();

    component.continue();
    await Promise.resolve();

    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(component.completionError()).toContain('réessayez');
  });
});

interface TutorialActions {
  readonly store: {
    next(): void;
    chooseWinner(entryId: number): void;
    step(): { readonly id: string };
    selectedEntryId(): number | null;
  };
  completionError(): string | null;
  continue(): void;
}
