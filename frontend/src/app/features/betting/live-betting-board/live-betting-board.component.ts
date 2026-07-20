import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { TuiButton } from '@taiga-ui/core';

import { LiveRace, LiveRunner } from '../models/betting.model';
import { BettingApiService } from '../services/betting-api.service';
import { PublicPartner } from '../models/partner.model';
import { PartnerService } from '../services/partner.service';
import { RaceInProgressOverlayComponent } from '../../../shared/ui';

@Component({
  selector: 'app-live-betting-board',
  imports: [TuiButton, RaceInProgressOverlayComponent],
  templateUrl: './live-betting-board.component.html',
  styleUrl: './live-betting-board.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LiveBettingBoardComponent {
  private readonly bettingApi = inject(BettingApiService);
  private readonly partnerService = inject(PartnerService);

  readonly race = signal<LiveRace | null>(null);
  readonly pendingEntryId = signal<number | null>(null);
  readonly changeCandidate = signal<LiveRunner | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly bettingOpen = computed(() => this.race()?.state === 'BETTING');
  readonly partners = signal<readonly PublicPartner[]>([]);

  constructor() {
    effect(() => this.race.set(this.bettingApi.liveRace()));
    void this.loadPartners();
  }

  private async loadPartners(): Promise<void> {
    try {
      this.partners.set(await this.partnerService.findVisible());
    } catch {
      this.partners.set([]);
    }
  }

  protected selectRunner(runner: LiveRunner): void {
    const race = this.race();
    if (race?.state !== 'BETTING' || this.pendingEntryId() !== null) return;

    if (race.userBet === null) {
      void this.submitBet(runner);
      return;
    }

    if (race.userBet.entryId !== runner.entryId) {
      this.changeCandidate.set(runner);
    }
  }

  protected cancelChange(): void {
    this.changeCandidate.set(null);
  }

  protected confirmChange(): void {
    const runner = this.changeCandidate();
    this.changeCandidate.set(null);
    if (runner !== null) void this.submitBet(runner);
  }

  protected stateTitle(race: LiveRace): string {
    switch (race.state) {
      case 'STANDBY': return 'Les partants sont en place.';
      case 'BET_STARTING': return 'Préparez votre choix.';
      case 'BETTING': return 'Les paris sont ouverts.';
      case 'BET_CLOSED': return 'Les paris sont clos.';
      case 'FINISHED': return 'Résultat officiel.';
    }
  }

  protected stateMessage(race: LiveRace): string {
    switch (race.state) {
      case 'STANDBY': return 'Découvrez les chevaux. Vous pourrez parier dès l’ouverture des paris.';
      case 'BET_STARTING': return 'Le départ des votes est imminent. Gardez votre favori en tête.';
      case 'BETTING': return race.userBet === null ? 'Touchez un cheval pour enregistrer instantanément votre pari.' : 'Votre pari est enregistré. Un changement réinitialisera votre heure de vote.';
      case 'BET_CLOSED': return 'Bonne chance !';
      case 'FINISHED': return race.userBet?.state === 'WON' ? 'Votre cheval remporte la course !' : 'La course est terminée. Retrouvez le classement ci-dessous.';
    }
  }

  protected formatTime(value: string): string {
    return new Intl.DateTimeFormat('fr-FR', {
      hour: '2-digit', minute: '2-digit', second: '2-digit'
    }).format(new Date(value));
  }

  private async submitBet(runner: LiveRunner): Promise<void> {
    const race = this.race();
    if (race === null) return;

    this.pendingEntryId.set(runner.entryId);
    this.actionError.set(null);
    try {
      this.race.set(await this.bettingApi.placeBet(race.raceId, runner.entryId));
    } catch {
      this.actionError.set('Le pari n’a pas pu être enregistré. Vérifiez que les votes sont toujours ouverts.');
    } finally {
      this.pendingEntryId.set(null);
    }
  }
}
