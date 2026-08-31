import { Injectable, inject } from '@angular/core';

import { AuthService } from '../auth/auth.service';
import { AppFeaturesService } from '../features/app-features.service';

@Injectable({ providedIn: 'root' })
export class TutorialService {
  private readonly auth = inject(AuthService);
  private readonly features = inject(AppFeaturesService);

  shouldStart(): boolean {
    const user = this.auth.user();
    return this.features.activeMode() === 'BETTING' && user !== null && user.tutorialCompleted !== true;
  }

  async complete(): Promise<void> {
    await this.auth.completeTutorial();
  }
}
