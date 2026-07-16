export interface BetHistoryEntry {
  readonly raceId: number;
  readonly raceName: string;
  readonly finishedAt: string;
  readonly selectedHorseName: string;
  readonly winningHorseName: string;
  readonly placedAt: string;
  readonly state: 'WON' | 'LOST';
  readonly speedRank: number | null;
}

export interface BetHistoryAvailability {
  readonly hasHistory: boolean;
}
