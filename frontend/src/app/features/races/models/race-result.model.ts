export interface RaceWinnerHistoryEntry {
  readonly raceId: string;
  readonly runnerId: string;
  readonly runnerName: string;
  readonly userId: string;
  readonly userDisplayName: string;
  readonly placedAt: string;
  readonly rank: number;
}

export interface RaceResult {
  readonly raceId: string;
  readonly winningRunnerId: string;
  readonly declaredAt: string;
  readonly winners: readonly RaceWinnerHistoryEntry[];
}
