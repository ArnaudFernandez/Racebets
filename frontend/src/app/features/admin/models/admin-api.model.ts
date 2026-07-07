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

export interface RaceEntryAdminResponse {
  readonly id: number;
  readonly raceId: number;
  readonly raceName: string;
  readonly horseId: number;
  readonly horseName: string;
  readonly horseNumber: number;
  readonly rank: number | null;
}

export interface RaceEntryAdminRequest {
  readonly raceId: number;
  readonly horseId: number;
  readonly horseNumber: number;
  readonly rank: number | null;
}
