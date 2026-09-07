import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { SecureImageDirective } from './secure-image.directive';

@Component({
  imports: [SecureImageDirective],
  template: '<img [appSecureImage]="source()" alt="Test" />'
})
class TestHostComponent {
  readonly source = signal<string | null>('/api/quizzes/image');
}

describe('SecureImageDirective', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    fixture = TestBed.createComponent(TestHostComponent);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the protected image as a blob and revokes its object URL', () => {
    const createObjectUrl = spyOn(URL, 'createObjectURL').and.returnValue('blob:quiz-image');
    const revokeObjectUrl = spyOn(URL, 'revokeObjectURL');

    fixture.detectChanges();
    http.expectOne('/api/quizzes/image').flush(new Blob(['image'], { type: 'image/png' }));
    fixture.detectChanges();

    expect(createObjectUrl).toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('img').getAttribute('src')).toBe('blob:quiz-image');

    fixture.destroy();
    expect(revokeObjectUrl).toHaveBeenCalledOnceWith('blob:quiz-image');
  });
});
