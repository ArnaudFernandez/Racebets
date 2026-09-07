import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { TUI_DARK_MODE, TuiButton, TuiRoot } from '@taiga-ui/core';
import { filter, map } from 'rxjs';

import { AuthService } from './core/auth/auth.service';
import { AppBrandingService } from './core/branding/app-branding.service';
import { AppFeaturesService } from './core/features/app-features.service';
import { BetHistoryService } from './features/betting/services/bet-history.service';
import { BettingApiService } from './features/betting/services/betting-api.service';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, TuiButton, TuiRoot],
  templateUrl: './app.component.html',
  styleUrl: './app.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent {
  protected readonly auth = inject(AuthService);
  protected readonly branding = inject(AppBrandingService);
  protected readonly features = inject(AppFeaturesService);
  protected readonly betHistory = inject(BetHistoryService);
  private readonly router = inject(Router);
  private readonly bettingApi = inject(BettingApiService);
  private readonly darkMode = inject(TUI_DARK_MODE);
  private readonly title = inject(Title);
  private readonly document = inject(DOCUMENT);
  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects)
    ),
    { initialValue: this.router.url }
  );
  readonly logoutDialogOpen = signal(false);
  readonly tutorialActive = computed(() => this.currentUrl().split('?')[0] === '/tutorial');
  readonly raceInProgressOnBettingPage = computed(
    () => this.currentUrl().split('?')[0] === '/' && this.bettingApi.liveRace()?.state === 'BET_CLOSED'
  );

  constructor() {
    this.darkMode.set(false);
    this.branding.startWatching();
    effect(() => {
      this.title.setTitle(this.branding.appName());
      this.updateApplicationIcons(this.branding.imageUrl());
      const olifanTheme = this.branding.theme() === 'OLIFAN_GROUP';
      this.document.documentElement.dataset['brandTheme'] = olifanTheme ? 'olifan-group' : 'default';
      this.document
        .querySelector<HTMLMetaElement>('meta[name="theme-color"]')
        ?.setAttribute('content', olifanTheme ? '#8d1d22' : '#00552f');
    });
    this.features.startWatching();
    effect(() => {
      if (this.auth.isAuthenticated() && this.features.bettingEnabled()) {
        this.bettingApi.startWatching();
      } else {
        this.bettingApi.stopWatching();
      }
    });
    effect(() => {
      if (this.auth.session() === null || !this.features.bettingEnabled()) {
        this.betHistory.clearAvailability();
      } else {
        this.betHistory.startWatching();
      }
    });
    effect(() => {
      if (!this.auth.isAuthenticated() || !this.features.ready()) return;
      const path = this.currentUrl().split('?')[0];
      const activePath = this.features.defaultPath();
      const modePaths = ['/', '/login', '/quiz', '/word-cloud', '/history', '/tutorial'];
      if (modePaths.includes(path) && path !== activePath && !(path === '/history' && activePath === '/')) {
        void this.router.navigateByUrl(activePath);
      }
    });
  }

  protected requestLogout(): void {
    this.logoutDialogOpen.set(true);
  }

  protected async confirmLogout(): Promise<void> {
    this.logoutDialogOpen.set(false);
    this.auth.logout();
    await this.router.navigateByUrl('/login');
  }

  private updateApplicationIcons(imageUrl: string): void {
    const iconLinks = this.document.querySelectorAll<HTMLLinkElement>(
      'link[rel="icon"], link[rel="shortcut icon"], link[rel="apple-touch-icon"]'
    );
    iconLinks.forEach((link) => link.setAttribute('href', imageUrl));
  }
}
