export type UserRole = 'ADMIN' | 'USER' | 'VIP';

export interface LoginRequest {
  readonly email: string;
  readonly accessCode?: string;
}

export interface RegisterRequest {
  readonly name: string;
  readonly surname: string;
  readonly email: string;
  readonly accessCode: string;
}

export interface UserProfile {
  readonly id: number;
  readonly name: string;
  readonly surname: string;
  readonly birthDate: string | null;
  readonly email: string;
  readonly present: boolean;
  readonly tutorialCompleted: boolean;
  readonly roles: readonly UserRole[];
}

export interface LoginResponse {
  readonly token: string;
  readonly tokenType: 'Bearer';
  readonly expiresIn: number;
  readonly roles: readonly UserRole[];
  readonly user: UserProfile;
}

export interface AuthProviders {
  readonly google: boolean;
}

export interface OAuthCodeExchangeRequest {
  readonly code: string;
  readonly accessCode?: string;
}

export interface AuthSession {
  readonly token: string;
  readonly tokenType: 'Bearer';
  readonly expiresAt: number;
  readonly roles: readonly UserRole[];
  readonly user: UserProfile;
}
