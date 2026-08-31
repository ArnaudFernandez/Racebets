import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTaiga } from '@taiga-ui/core';

import { WordCloudQuestionListItem } from '../models/word-cloud.model';
import { WordCloudApiService } from '../services/word-cloud-api.service';
import { AdminWordCloudPanelComponent } from './admin-word-cloud-panel.component';

class AdminWordCloudApiStub {
  findQuestions(): Promise<readonly WordCloudQuestionListItem[]> {
    return Promise.resolve([]);
  }
}

describe('AdminWordCloudPanelComponent', () => {
  it('offers full-screen control and a confirmed reset for completed questions', async () => {
    await TestBed.configureTestingModule({
      imports: [AdminWordCloudPanelComponent],
      providers: [
        { provide: WordCloudApiService, useClass: AdminWordCloudApiStub },
        provideRouter([]),
        provideTaiga()
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(AdminWordCloudPanelComponent);
    const component = fixture.componentInstance;
    component.questions.set([question(1, 'DRAFT'), question(2, 'OPEN'), question(3, 'CLOSED')]);
    component.initialLoading.set(false);
    fixture.detectChanges();

    const rows = [...fixture.nativeElement.querySelectorAll('tbody tr')] as HTMLTableRowElement[];
    expect(rows[0].textContent).toContain('Piloter');
    expect(rows[0].querySelectorAll('button').length).toBe(3);
    expect(rows[1].textContent).toContain('Piloter');
    expect(rows[1].querySelectorAll('button').length).toBe(1);
    expect(rows[2].textContent).toContain('Réinitialiser cette question');
    expect(rows[2].querySelectorAll('button').length).toBe(2);
    expect(rows[2].classList).toContain('finished-row');

    fixture.destroy();
  });
});

function question(id: number, status: WordCloudQuestionListItem['status']): WordCloudQuestionListItem {
  return {
    id,
    text: `Question ${id}`,
    status,
    submissionCount: status === 'DRAFT' ? 0 : 8,
    createdAt: '2026-08-31T11:00:00Z',
    openedAt: status === 'DRAFT' ? null : '2026-08-31T12:00:00Z',
    revealedAt: null,
    closedAt: status === 'CLOSED' ? '2026-08-31T13:00:00Z' : null
  };
}
