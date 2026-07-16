import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TUI_DARK_MODE, TuiButton, TuiRoot } from '@taiga-ui/core';

import { AuthService } from './core/auth/auth.service';
import { AppFeaturesService } from './core/features/app-features.service';
import { BetHistoryService } from './features/betting/services/bet-history.service';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, TuiButton, TuiRoot],
  templateUrl: './app.component.html',
  styleUrl: './app.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent {
  protected readonly auth = inject(AuthService);
  protected readonly features = inject(AppFeaturesService);
  protected readonly betHistory = inject(BetHistoryService);
  private readonly router = inject(Router);
  private readonly darkMode = inject(TUI_DARK_MODE);
  readonly logoutDialogOpen = signal(false);

  constructor() {
    this.darkMode.set(false);
    void this.features.load();
    effect(() => {
      if (this.auth.session() === null) {
        this.betHistory.clearAvailability();
      } else {
        this.betHistory.startWatching();
      }
    });
  }

  protected requestLogout(): void {
    this.logoutDialogOpen.set(true);
  }

  protected async confirmLogout(): Promise<void> {
    this.logoutDialogOpen.set(false);
    this.auth.logout();
    await this.router.navigateByUrl(this.features.defaultPath());
  }
}
