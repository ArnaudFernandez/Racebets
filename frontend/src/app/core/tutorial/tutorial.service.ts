import { Injectable, inject } from '@angular/core';

import { AuthService } from '../auth/auth.service';

@Injectable({ providedIn: 'root' })
export class TutorialService {
  private readonly auth = inject(AuthService);

  shouldStart(): boolean {
    const user = this.auth.user();
    return user !== null && user.tutorialCompleted !== true;
  }

  async complete(): Promise<void> {
    await this.auth.completeTutorial();
  }
}
