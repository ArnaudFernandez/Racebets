import { ComponentFixture, TestBed, discardPeriodicTasks, fakeAsync, flushMicrotasks } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { provideTaiga } from '@taiga-ui/core';

import { QuizSessionSnapshotResponse } from '../models/quiz-api.model';
import { QuizApiService } from '../services/quiz-api.service';
import { QuizControlPageComponent } from './quiz-control-page.component';

describe('QuizControlPageComponent', () => {
  let fixture: ComponentFixture<QuizControlPageComponent>;
  let router: Router;

  const activeSession: QuizSessionSnapshotResponse = {
    id: 12,
    quizSetId: 3,
    title: 'Grand quiz des courses',
    phase: 'OPENING',
    currentQuestionIndex: -1,
    questionCount: 5,
    participantCount: 3,
    serverTime: new Date().toISOString(),
    phaseStartedAt: new Date().toISOString(),
    questionEndsAt: null,
    joined: false,
    selectedAnswerId: null,
    correctAnswerId: null,
    submittedAnswers: 0,
    currentQuestion: null,
    scores: []
  };
  const cancelledSession = { ...activeSession, phase: 'CANCELLED' as const };
  const quizApi = {
    findAdminSession: jasmine.createSpy('findAdminSession').and.resolveTo(activeSession),
    stopSession: jasmine.createSpy('stopSession').and.resolveTo(cancelledSession),
    startSession: jasmine.createSpy('startSession'),
    lockQuestion: jasmine.createSpy('lockQuestion'),
    revealAnswer: jasmine.createSpy('revealAnswer'),
    showScoreboard: jasmine.createSpy('showScoreboard'),
    nextQuestion: jasmine.createSpy('nextQuestion')
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QuizControlPageComponent],
      providers: [
        provideRouter([]),
        provideTaiga(),
        { provide: QuizApiService, useValue: quizApi },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ sessionId: '12' }) } }
        }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
  });

  afterEach(() => {
    quizApi.findAdminSession.calls.reset();
    quizApi.stopSession.calls.reset();
  });

  it('asks for confirmation before stopping the active quiz', fakeAsync(() => {
    fixture = TestBed.createComponent(QuizControlPageComponent);
    fixture.detectChanges();
    flushMicrotasks();
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    const button = root.querySelector<HTMLButtonElement>('.control-actions button');
    expect(button?.textContent).toContain('Arrêter le quiz');

    button?.click();
    expect(fixture.componentInstance.stopQuizDialogOpen()).toBeTrue();
    expect(quizApi.stopSession).not.toHaveBeenCalled();

    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('stops the quiz and returns to the quiz list after confirmation', fakeAsync(() => {
    const navigate = spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(QuizControlPageComponent);
    fixture.detectChanges();
    flushMicrotasks();

    fixture.componentInstance['requestQuizStop']();
    void fixture.componentInstance['confirmQuizStop']();
    flushMicrotasks();

    expect(quizApi.stopSession).toHaveBeenCalledOnceWith(12);
    expect(navigate).toHaveBeenCalledOnceWith(['/admin'], { queryParams: { section: 'quizzes' } });
    expect(fixture.componentInstance.stopQuizDialogOpen()).toBeFalse();

    fixture.destroy();
    discardPeriodicTasks();
  }));
});
