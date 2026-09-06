export type RaceState =
  | 'CREATED'
  | 'STANDBY'
  | 'BET_STARTING'
  | 'BETTING'
  | 'BET_CLOSED'
  | 'FINISHED';

export type UserRole = 'ADMIN' | 'USER' | 'VIP';

export interface AdminUserResponse {
  readonly id: number;
  readonly name: string;
  readonly surname: string;
  readonly birthDate: string | null;
  readonly email: string;
  readonly present: boolean;
  readonly roles: readonly UserRole[];
}

export interface AdminUserRequest {
  readonly name: string;
  readonly surname: string;
  readonly birthDate: string | null;
  readonly email: string;
  readonly accessCode: string | null;
  readonly present: boolean;
  readonly roles: readonly UserRole[];
}

export type UserImportAction = 'CREATE' | 'UPDATE' | 'UNCHANGED' | 'ERROR';

export interface UserImportRow {
  readonly lineNumber: number;
  readonly email: string;
  readonly name: string;
  readonly surname: string;
  readonly action: UserImportAction;
  readonly warning: string | null;
  readonly error: string | null;
}

export interface UserImportPreview {
  readonly fileDigest: string;
  readonly planFingerprint: string;
  readonly totalRows: number;
  readonly createCount: number;
  readonly updateCount: number;
  readonly unchangedCount: number;
  readonly errorCount: number;
  readonly importable: boolean;
  readonly rows: readonly UserImportRow[];
}

export interface UserImportResult {
  readonly createdCount: number;
  readonly updatedCount: number;
  readonly unchangedCount: number;
}

export interface HorseAdminResponse {
  readonly id: number;
  readonly name: string;
}

export interface HorseAdminRequest {
  readonly name: string;
}

export interface RaceAdminResponse {
  readonly id: number;
  readonly name: string;
  readonly raceImgUrl: string | null;
  readonly state: RaceState;
}

export interface RaceAdminRequest {
  readonly name: string;
  readonly raceImgUrl: string | null;
  readonly state: RaceState | null;
}

export interface RaceControlEntry {
  readonly entryId: number;
  readonly horseId: number;
  readonly horseName: string;
  readonly horseNumber: number;
  readonly rank: number | null;
  readonly betCount: number;
}

export interface RaceControl {
  readonly id: number;
  readonly name: string;
  readonly raceImgUrl: string | null;
  readonly state: RaceState;
  readonly visibleOnLive: boolean;
  readonly entries: readonly RaceControlEntry[];
  readonly totalBets: number;
  readonly updatedAt: string;
}

export interface RaceHistorySummary {
  readonly raceId: number;
  readonly raceName: string;
  readonly finishedAt: string;
  readonly runnerCount: number;
  readonly totalVotes: number;
  readonly winnerCount: number;
  readonly winningHorseName: string;
}

export interface RaceHistoryResult {
  readonly entryId: number;
  readonly rank: number;
  readonly horseNumber: number;
  readonly horseName: string;
  readonly voteCount: number;
}

export interface RaceHistoryVote {
  readonly betId: number;
  readonly userId: number;
  readonly userDisplayName: string;
  readonly userEmail: string;
  readonly horseName: string;
  readonly placedAt: string;
  readonly state: 'WON' | 'LOST';
}

export interface RaceHistoryWinner {
  readonly speedRank: number;
  readonly userId: number;
  readonly userDisplayName: string;
  readonly userEmail: string;
  readonly horseName: string;
  readonly placedAt: string;
}

export interface RaceHistoryDetail {
  readonly raceId: number;
  readonly raceName: string;
  readonly finishedAt: string;
  readonly totalVotes: number;
  readonly result: readonly RaceHistoryResult[];
  readonly winners: readonly RaceHistoryWinner[];
  readonly votes: readonly RaceHistoryVote[];
}

export interface PartnerAdminResponse {
  readonly id: number;
  readonly name: string;
  readonly displayOnWaiting: boolean;
  readonly logoUrl: string;
}
