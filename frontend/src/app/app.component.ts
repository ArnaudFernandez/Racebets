import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TUI_DARK_MODE, TuiButton, TuiRoot } from '@taiga-ui/core';

import { AuthService } from './core/auth/auth.service';
import { AppFeaturesService } from './core/features/app-features.service';

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
  private readonly router = inject(Router);
  private readonly darkMode = inject(TUI_DARK_MODE);

  constructor() {
    this.darkMode.set(false);
    void this.features.load();
  }

  protected async logout(): Promise<void> {
    this.auth.logout();
    await this.router.navigateByUrl(this.features.defaultPath());
  }
}
