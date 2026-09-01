import { signal } from '@angular/core';
import {
  ComponentFixture,
  TestBed,
  discardPeriodicTasks,
  fakeAsync,
  flushMicrotasks
} from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTaiga } from '@taiga-ui/core';

import { AuthService } from '../../../core/auth/auth.service';
import { AppFeaturesService } from '../../../core/features/app-features.service';
import { QuizSessionPhase, QuizSessionSnapshotResponse } from '../models/quiz-api.model';
import { QuizApiService } from '../services/quiz-api.service';
import { QuizPlayPageComponent } from './quiz-play-page.component';

describe('QuizPlayPageComponent', () => {
  let fixture: ComponentFixture<QuizPlayPageComponent>;
  let activeSnapshot: QuizSessionSnapshotResponse;

  const quizApi = {
    findLiveSessions: jasmine
      .createSpy('findLiveSessions')
      .and.callFake(async () => [activeSnapshot]),
    findSession: jasmine.createSpy('findSession').and.callFake(async () => activeSnapshot),
    joinSession: jasmine.createSpy('joinSession').and.callFake(async () => activeSnapshot),
    answer: jasmine.createSpy('answer').and.callFake(async () => activeSnapshot)
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QuizPlayPageComponent],
      providers: [
        provideRouter([]),
        provideTaiga(),
        { provide: QuizApiService, useValue: quizApi },
        {
          provide: AuthService,
          useValue: {
            user: signal({
              id: 42,
              name: 'Camille',
              surname: 'Martin',
              birthDate: null,
              email: 'camille@example.com',
              present: true,
              tutorialCompleted: true,
              roles: ['USER']
            })
          }
        },
        {
          provide: AppFeaturesService,
          useValue: {
            ensureLoaded: async () => ({ activeMode: 'QUIZ' }),
            defaultPath: () => '/quiz'
          }
        }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    quizApi.findLiveSessions.calls.reset();
    quizApi.findSession.calls.reset();
    quizApi.joinSession.calls.reset();
    quizApi.answer.calls.reset();
  });

  it('presents the image and four large answer choices during a question', fakeAsync(() => {
    renderPhase('QUESTION_OPEN', null, null);

    const root = fixture.nativeElement as HTMLElement;
    const answers = root.querySelectorAll<HTMLButtonElement>('.answer-card');

    expect(root.querySelector('.question-media img')).not.toBeNull();
    expect(root.querySelector('.timer-progress')).not.toBeNull();
    expect(answers.length).toBe(4);
    expect(answers[0].getAttribute('aria-pressed')).toBe('false');
    expect(root.querySelector('.quiz-scoreboard-stage')).toBeNull();

    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('presents a compact waiting scene before the first question', fakeAsync(() => {
    const openingSnapshot = snapshot('OPENING', null, null, true);

    renderSnapshot({
      ...openingSnapshot,
      currentQuestionIndex: -1,
      currentQuestion: null,
      questionEndsAt: null
    });

    const root = fixture.nativeElement as HTMLElement;
    const shell = root.querySelector<HTMLElement>('.quiz-shell');

    expect(root.querySelector('.live-header')).not.toBeNull();
    expect(root.querySelector('.waiting-state')).not.toBeNull();
    expect(root.querySelector('.game-stage')).toBeNull();
    expect(root.querySelector('.quiz-scoreboard-stage')).toBeNull();
    expect(getComputedStyle(shell!).alignContent).toBe('start');

    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('keeps the join invitation ahead of every live scene for a late participant', fakeAsync(() => {
    renderPhase('QUESTION_OPEN', null, null, false);

    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelector('.welcome-state')).not.toBeNull();
    expect(root.querySelector('.game-stage')).toBeNull();
    expect(root.querySelector('.locked-stage')).toBeNull();
    expect(root.querySelector('.reveal-stage')).toBeNull();

    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('supports a text-only question with two answer choices', fakeAsync(() => {
    const questionSnapshot = snapshot('QUESTION_OPEN', null, null, true);

    renderSnapshot({
      ...questionSnapshot,
      currentQuestion: {
        ...questionSnapshot.currentQuestion!,
        questionImageDataUrl: null,
        answers: questionSnapshot.currentQuestion!.answers.slice(0, 2)
      }
    });

    const root = fixture.nativeElement as HTMLElement;
    const questionBoard = root.querySelector('.question-board');

    expect(questionBoard?.classList).not.toContain('has-media');
    expect(root.querySelector('.question-media')).toBeNull();
    expect(root.querySelectorAll('.answer-card').length).toBe(2);
    expect(root.querySelectorAll('.answer-letter')[1].textContent).toContain('B');

    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('submits the first answer without asking for confirmation', fakeAsync(() => {
    renderPhase('QUESTION_OPEN', null, null);

    const root = fixture.nativeElement as HTMLElement;
    const answers = root.querySelectorAll<HTMLButtonElement>('.answer-card');

    answers[1].click();
    flushMicrotasks();
    fixture.detectChanges();

    expect(quizApi.answer).toHaveBeenCalledOnceWith(7, 12);
    expect(fixture.componentInstance.answerChangeCandidate()).toBeNull();

    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('requires confirmation before replacing an existing answer', fakeAsync(() => {
    renderPhase('QUESTION_OPEN', 11, null);

    const root = fixture.nativeElement as HTMLElement;
    const answers = root.querySelectorAll<HTMLButtonElement>('.answer-card');
    const actions = fixture.componentInstance as unknown as {
      cancelAnswerChange(): void;
      confirmAnswerChange(): void;
    };

    answers[1].click();

    expect(fixture.componentInstance.answerChangeCandidate()?.id).toBe(12);
    expect(quizApi.answer).not.toHaveBeenCalled();

    actions.cancelAnswerChange();
    expect(fixture.componentInstance.answerChangeCandidate()).toBeNull();
    expect(quizApi.answer).not.toHaveBeenCalled();

    answers[1].click();
    actions.confirmAnswerChange();
    flushMicrotasks();
    fixture.detectChanges();

    expect(quizApi.answer).toHaveBeenCalledOnceWith(7, 12);
    expect(fixture.componentInstance.answerChangeCandidate()).toBeNull();

    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('shows the locked scene even when no answer was submitted', fakeAsync(() => {
    renderPhase('QUESTION_LOCKED', null, null);

    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelector('.locked-stage')).not.toBeNull();
    expect(root.querySelector('.locked-choice.is-empty')?.textContent).toContain(
      'Aucune réponse enregistrée'
    );
    expect(root.querySelector('.answer-grid')).toBeNull();

    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('replaces the question choices with a focused correction after reveal', fakeAsync(() => {
    renderPhase('ANSWER_REVEALED', 12, 11);

    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelector('.reveal-stage')).not.toBeNull();
    expect(root.querySelector('.answer-media img')).not.toBeNull();
    expect(root.querySelector('.correct-answer')?.textContent).toContain('Casaque verte');
    expect(root.querySelector('.your-answer')?.textContent).toContain('Casaque bleue');
    expect(root.querySelector('.answer-grid')).toBeNull();
    expect(root.textContent).not.toContain('Quelle casaque franchit la ligne en tête ?');

    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('adapts the correction to correct and unanswered results without media', fakeAsync(() => {
    for (const result of [
      { selectedAnswerId: 11, className: 'is-correct', title: 'Bien joué !' },
      { selectedAnswerId: null, className: 'is-unanswered', title: 'Temps écoulé' }
    ] as const) {
      const revealSnapshot = snapshot('ANSWER_REVEALED', result.selectedAnswerId, 11, true);

      renderSnapshot({
        ...revealSnapshot,
        currentQuestion: {
          ...revealSnapshot.currentQuestion!,
          answerImageDataUrl: null
        }
      });

      const root = fixture.nativeElement as HTMLElement;
      const reveal = root.querySelector('.reveal-stage');

      expect(reveal?.classList).toContain(result.className);
      expect(root.querySelector('.reveal-heading')?.textContent).toContain(result.title);
      expect(root.querySelector('.answer-media')).toBeNull();
      expect(root.querySelector('.your-answer')).toBeNull();

      fixture.destroy();
    }

    discardPeriodicTasks();
  }));

  it('shows only the full-screen leaderboard between rounds', fakeAsync(() => {
    renderPhase('SCOREBOARD', 12, 11);

    const root = fixture.nativeElement as HTMLElement;
    const stage = root.querySelector('.quiz-scoreboard-stage');

    expect(stage).not.toBeNull();
    expect(root.querySelector('.live-header')).toBeNull();
    expect(root.querySelector('.question-board')).toBeNull();
    expect(root.querySelector('.reveal-stage')).toBeNull();
    expect(root.querySelectorAll('.leaderboard li').length).toBe(3);
    expect(root.querySelector('.player-summary')?.textContent).toContain('2e');
    expect(root.querySelector('.summary-score strong')?.textContent).toBe('2');
    expect(root.querySelector('.summary-score span')?.textContent).toBe('points');
    expect(root.textContent).not.toContain('Quelle casaque franchit la ligne en tête ?');
    expect(document.activeElement).toBe(root.querySelector('#scoreboard-title'));

    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('shows the leaderboard to a late viewer who has not joined', fakeAsync(() => {
    renderPhase('SCOREBOARD', null, 11, false);

    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelector('.quiz-scoreboard-stage')).not.toBeNull();
    expect(root.querySelector('.welcome-state')).toBeNull();
    expect(root.querySelectorAll('.leaderboard li').length).toBe(3);

    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('keeps an explicit empty state in the full-screen leaderboard', fakeAsync(() => {
    const scoreboardSnapshot = snapshot('SCOREBOARD', null, 11, true);

    renderSnapshot({ ...scoreboardSnapshot, scores: [] });

    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelector('.quiz-scoreboard-stage')).not.toBeNull();
    expect(root.querySelector('.player-summary')).toBeNull();
    expect(root.querySelectorAll('.leaderboard li').length).toBe(1);
    expect(root.querySelector('.empty-score')?.textContent).toContain('Le classement apparaîtra');

    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('clears every live scene when the session finishes', fakeAsync(() => {
    const finishedSnapshot = snapshot('FINISHED', 11, 11, true);

    renderSnapshot({
      ...finishedSnapshot,
      currentQuestion: null,
      questionEndsAt: null,
      scores: []
    });

    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelector('.live-header')).toBeNull();
    expect(root.querySelector('.game-stage')).toBeNull();
    expect(root.querySelector('.reveal-stage')).toBeNull();
    expect(root.querySelector('.quiz-scoreboard-stage')).toBeNull();
    expect(root.querySelector('.empty-session')).not.toBeNull();

    fixture.destroy();
    discardPeriodicTasks();
  }));

  function renderPhase(
    phase: QuizSessionPhase,
    selectedAnswerId: number | null,
    correctAnswerId: number | null,
    joined = true
  ): void {
    renderSnapshot(snapshot(phase, selectedAnswerId, correctAnswerId, joined));
  }

  function renderSnapshot(value: QuizSessionSnapshotResponse): void {
    activeSnapshot = value;
    fixture = TestBed.createComponent(QuizPlayPageComponent);
    fixture.detectChanges();
    flushMicrotasks();
    fixture.detectChanges();
  }

  function snapshot(
    phase: QuizSessionPhase,
    selectedAnswerId: number | null,
    correctAnswerId: number | null,
    joined: boolean
  ): QuizSessionSnapshotResponse {
    const serverTime = new Date().toISOString();

    return {
      id: 7,
      quizSetId: 3,
      title: 'Grand quiz des courses',
      phase,
      currentQuestionIndex: 1,
      questionCount: 5,
      participantCount: 3,
      serverTime,
      phaseStartedAt: serverTime,
      questionEndsAt: new Date(Date.now() + 30_000).toISOString(),
      joined,
      selectedAnswerId,
      correctAnswerId,
      submittedAnswers: 3,
      currentQuestion: {
        id: 5,
        position: 1,
        text: 'Quelle casaque franchit la ligne en tête ?',
        questionImageDataUrl: 'data:image/png;base64,question',
        answerImageDataUrl: 'data:image/png;base64,answer',
        durationSeconds: 30,
        answers: [
          {
            id: 11,
            position: 0,
            text: 'Casaque verte',
            correct: phase === 'QUESTION_OPEN' ? null : true
          },
          {
            id: 12,
            position: 1,
            text: 'Casaque bleue',
            correct: phase === 'QUESTION_OPEN' ? null : false
          },
          {
            id: 13,
            position: 2,
            text: 'Casaque rouge',
            correct: phase === 'QUESTION_OPEN' ? null : false
          },
          {
            id: 14,
            position: 3,
            text: 'Casaque jaune',
            correct: phase === 'QUESTION_OPEN' ? null : false
          }
        ]
      },
      scores: [
        { userId: 8, displayName: 'Alex Bernard', score: 3 },
        { userId: 42, displayName: 'Camille Martin', score: 2 },
        { userId: 19, displayName: 'Nora Petit', score: 1 }
      ]
    };
  }
});
