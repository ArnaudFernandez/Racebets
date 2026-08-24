import { ChangeDetectionStrategy, Component, ElementRef, effect, inject, signal, viewChild, viewChildren } from '@angular/core';
import { Router } from '@angular/router';
import { TuiButton, TuiDialogService } from '@taiga-ui/core';
import { TUI_CONFIRM, TuiButtonLoading, TuiConfirmData, TuiProgress } from '@taiga-ui/kit';
import { firstValueFrom } from 'rxjs';

import { TutorialService } from '../../../core/tutorial/tutorial.service';
import {
  TUTORIAL_HISTORY,
  TUTORIAL_RUNNERS,
  TUTORIAL_STEPS,
  TUTORIAL_WINNER_ID,
  TutorialRunner,
  TutorialTarget
} from '../tutorial.fixture';
import { TutorialStore } from '../tutorial.store';

@Component({
  selector: 'app-tutorial-page',
  imports: [TuiButton, TuiButtonLoading, TuiProgress],
  providers: [TutorialStore],
  templateUrl: './tutorial-page.component.html',
  styleUrl: './tutorial-page.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TutorialPageComponent {
  protected readonly store = inject(TutorialStore);
  protected readonly runners = TUTORIAL_RUNNERS;
  protected readonly history = TUTORIAL_HISTORY;
  protected readonly stepsCount = TUTORIAL_STEPS.length;
  protected readonly winnerId = TUTORIAL_WINNER_ID;
  protected readonly completing = signal(false);
  protected readonly completionError = signal<string | null>(null);

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly tutorial = inject(TutorialService);
  private readonly router = inject(Router);
  private readonly dialogs = inject(TuiDialogService);
  private readonly coachTitle = viewChild<ElementRef<HTMLHeadingElement>>('coachTitle');
  private readonly runnerButtons = viewChildren<ElementRef<HTMLButtonElement>>('runnerButton');

  constructor() {
    effect(() => {
      const target = this.store.step().target;
      queueMicrotask(() => {
        this.host.nativeElement.querySelector<HTMLElement>('.spotlight')?.scrollIntoView({ block: 'center' });
        const element = target === 'runner' ? this.runnerButtons()[1] : this.coachTitle();
        element?.nativeElement.focus({ preventScroll: true });
      });
    });
  }

  protected isSpotlight(target: TutorialTarget): boolean {
    return this.store.step().target === target;
  }

  protected selectRunner(runner: TutorialRunner): void {
    this.store.chooseWinner(runner.entryId);
  }

  protected continue(): void {
    if (this.store.step().action === 'finish') {
      void this.leaveTutorial();
      return;
    }

    this.store.next();
  }

  protected async requestExit(): Promise<void> {
    const data: TuiConfirmData = {
      content: 'Vous pourrez utiliser l’application normalement, mais cette visite guidée sera considérée comme terminée.',
      yes: 'Quitter le tutoriel',
      no: 'Continuer la visite',
      appearance: 'primary-destructive'
    };
    const confirmed = await firstValueFrom(
      this.dialogs.open<boolean>(TUI_CONFIRM, {
        label: 'Quitter le tutoriel ?',
        size: 's',
        data
      }),
      { defaultValue: false }
    );

    if (confirmed) {
      await this.leaveTutorial();
    }
  }

  private async leaveTutorial(): Promise<void> {
    this.completing.set(true);
    this.completionError.set(null);
    try {
      await this.tutorial.complete();
      await this.router.navigateByUrl('/');
    } catch {
      this.completionError.set('La fin du tutoriel n’a pas pu être enregistrée. Vérifiez votre connexion puis réessayez.');
    } finally {
      this.completing.set(false);
    }
  }
}
