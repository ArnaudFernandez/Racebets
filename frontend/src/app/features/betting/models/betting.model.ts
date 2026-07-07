export interface RaceRunner {
  readonly runnerId: string;
  readonly runnerName: string;
  readonly updatedAt: string;
}

export interface RaceBettingUpdate {
  readonly raceId: string;
  readonly bettingOpenedAt: string;
  readonly runners: readonly RaceRunner[];
}

export interface HorseBetSelection {
  readonly raceId: string;
  readonly runnerId: string;
  readonly runnerName: string;
  readonly placedAt: string;
}

export interface UserRaceBet {
  readonly selection: HorseBetSelection | null;
}
