import { HttpClient } from '@angular/common/http';
import { Directive, effect, inject, input, signal } from '@angular/core';

@Directive({
  selector: 'img[appSecureImage]',
  host: {
    '[attr.src]': 'objectUrl()'
  }
})
export class SecureImageDirective {
  private readonly http = inject(HttpClient);
  readonly source = input<string | null>(null, { alias: 'appSecureImage' });
  protected readonly objectUrl = signal<string | null>(null);

  constructor() {
    effect((onCleanup) => {
      const source = this.source();
      let createdUrl: string | null = null;
      this.objectUrl.set(null);
      if (source === null) return;

      const subscription = this.http.get(source, { responseType: 'blob' }).subscribe({
        next: (blob) => {
          createdUrl = URL.createObjectURL(blob);
          this.objectUrl.set(createdUrl);
        },
        error: () => this.objectUrl.set(null)
      });

      onCleanup(() => {
        subscription.unsubscribe();
        if (createdUrl !== null) URL.revokeObjectURL(createdUrl);
      });
    });
  }
}
