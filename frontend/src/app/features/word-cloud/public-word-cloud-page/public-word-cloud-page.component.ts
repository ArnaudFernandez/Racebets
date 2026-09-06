import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject } from '@angular/core';

import { WordCloudApiService } from '../services/word-cloud-api.service';
import { presentWord } from '../word-cloud-presentation';

@Component({
  selector: 'app-public-word-cloud-page',
  templateUrl: './public-word-cloud-page.component.html',
  styleUrl: './public-word-cloud-page.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PublicWordCloudPageComponent {
  private readonly api = inject(WordCloudApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly snapshot = this.api.publicLiveSnapshot;
  readonly cloudWords = computed(() => {
    const words = this.snapshot()?.words ?? [];
    const maxCount = words.reduce((maximum, word) => Math.max(maximum, word.count), 1);
    return words.map((word) => presentWord(word, maxCount));
  });

  constructor() {
    this.api.startPublicPolling(this.destroyRef);
  }
}
