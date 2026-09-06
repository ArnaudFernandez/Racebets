import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, flushMicrotasks, tick } from '@angular/core/testing';
import { TimeoutError } from 'rxjs';

import { QuizSetRequest } from '../models/quiz-api.model';
import { QuizApiService } from './quiz-api.service';

describe('QuizApiService', () => {
  let service: QuizApiService;
  let http: HttpTestingController;

  const request: QuizSetRequest = {
    title: 'Grand quiz',
    questions: [
      {
        text: 'Quelle casaque gagne ?',
        questionImageDataUrl: 'data:image/png;base64,aW1hZ2U=',
        answerImageDataUrl: null,
        durationSeconds: 30,
        answers: [
          { text: 'Verte', correct: true },
          { text: 'Bleue', correct: false }
        ]
      }
    ]
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(QuizApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends quiz images in the create request', async () => {
    const pending = service.createQuizSet(request);
    const httpRequest = http.expectOne('/api/admin/quizzes');

    expect(httpRequest.request.method).toBe('POST');
    expect(httpRequest.request.body).toEqual(request);
    httpRequest.flush({ id: 7, title: request.title, questions: [] });

    await expectAsync(pending).toBeResolved();
  });

  it('cancels a quiz save that exceeds one minute', fakeAsync(() => {
    let failure: unknown;
    void service.createQuizSet(request).catch((error: unknown) => (failure = error));
    const httpRequest = http.expectOne('/api/admin/quizzes');

    tick(60_000);
    flushMicrotasks();

    expect(failure).toEqual(jasmine.any(TimeoutError));
    expect(httpRequest.cancelled).toBeTrue();
  }));

  it('stops an active quiz session', async () => {
    const pending = service.stopSession(12);
    const httpRequest = http.expectOne('/api/admin/quizzes/sessions/12/stop');

    expect(httpRequest.request.method).toBe('POST');
    expect(httpRequest.request.body).toEqual({});
    httpRequest.flush({ phase: 'CANCELLED' });

    await expectAsync(pending).toBeResolved();
  });
});
