export type LiveRaceState = 'STANDBY' | 'BET_STARTING' | 'BETTING' | 'BET_CLOSED' | 'FINISHED';
export type UserBetState = 'PENDING' | 'WON' | 'LOST';

export interface LiveRunner {
  readonly entryId: number;
  readonly horseId: number;
  readonly horseName: string;
  readonly horseNumber: number;
  readonly rank: number | null;
  readonly betCount: number;
}

export interface LiveUserBet {
  readonly entryId: number;
  readonly horseName: string;
  readonly placedAt: string;
  readonly state: UserBetState;
  readonly speedRank: number | null;
}

export interface LiveRace {
  readonly raceId: number;
  readonly raceName: string;
  readonly raceImgUrl: string | null;
  readonly state: LiveRaceState;
  readonly runners: readonly LiveRunner[];
  readonly userBet: LiveUserBet | null;
  readonly totalBets: number;
  readonly updatedAt: string;
}
