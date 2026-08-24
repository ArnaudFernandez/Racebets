import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import {
  AuthProviders,
  AuthSession,
  LoginRequest,
  LoginResponse,
  OAuthCodeExchangeRequest,
  RegisterRequest,
  UserProfile,
  UserRole
} from './auth.model';

const STORAGE_KEY = 'racebets.auth.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly sessionState = signal<AuthSession | null>(this.readStoredSession());

  readonly session = this.sessionState.asReadonly();
  readonly isAuthenticated = computed(() => this.sessionState() !== null);
  readonly isAdmin = computed(() => this.sessionState()?.roles.includes('ADMIN') ?? false);
  readonly user = computed(() => this.sessionState()?.user ?? null);

  async login(request: LoginRequest): Promise<AuthSession> {
    const response = await firstValueFrom(this.http.post<LoginResponse>('/api/auth/login', request));
    const session = this.toSession(response);
    this.storeSession(session);
    return session;
  }

  async register(request: RegisterRequest): Promise<AuthSession> {
    const response = await firstValueFrom(this.http.post<LoginResponse>('/api/auth/register', request));
    const session = this.toSession(response);
    this.storeSession(session);
    return session;
  }

  async providers(): Promise<AuthProviders> {
    return firstValueFrom(this.http.get<AuthProviders>('/api/auth/providers'));
  }

  async exchangeOAuthCode(request: OAuthCodeExchangeRequest): Promise<AuthSession> {
    const response = await firstValueFrom(this.http.post<LoginResponse>('/api/auth/oauth/exchange', request));
    const session = this.toSession(response);
    this.storeSession(session);
    return session;
  }

  async refreshUser(): Promise<UserProfile | null> {
    if (this.sessionState() === null) {
      return null;
    }

    const user = await firstValueFrom(this.http.get<UserProfile>('/api/auth/me'));
    const session = this.sessionState();
    if (session === null) {
      return null;
    }

    this.storeSession({ ...session, roles: user.roles, user });
    return user;
  }

  async completeTutorial(): Promise<UserProfile> {
    const user = await firstValueFrom(this.http.post<UserProfile>('/api/auth/me/tutorial-completion', null));
    const session = this.sessionState();
    if (session !== null) {
      this.storeSession({ ...session, user });
    }
    return user;
  }

  logout(): void {
    this.sessionState.set(null);
    this.storage()?.removeItem(STORAGE_KEY);
  }

  authorizationHeader(): string | null {
    const session = this.sessionState();

    if (session === null) {
      return null;
    }

    if (session.expiresAt <= Date.now()) {
      this.logout();
      return null;
    }

    return `${session.tokenType} ${session.token}`;
  }

  private storeSession(session: AuthSession): void {
    this.sessionState.set(session);
    this.storage()?.setItem(STORAGE_KEY, JSON.stringify(session));
  }

  private toSession(response: LoginResponse): AuthSession {
    return {
      token: response.token,
      tokenType: response.tokenType,
      expiresAt: Date.now() + response.expiresIn,
      roles: response.roles,
      user: response.user
    };
  }

  private readStoredSession(): AuthSession | null {
    const raw = this.storage()?.getItem(STORAGE_KEY);

    if (raw === undefined || raw === null) {
      return null;
    }

    try {
      const parsed: unknown = JSON.parse(raw);

      if (!this.isAuthSession(parsed) || parsed.expiresAt <= Date.now()) {
        this.storage()?.removeItem(STORAGE_KEY);
        return null;
      }

      return parsed;
    } catch {
      this.storage()?.removeItem(STORAGE_KEY);
      return null;
    }
  }

  private isAuthSession(value: unknown): value is AuthSession {
    if (typeof value !== 'object' || value === null) {
      return false;
    }

    const candidate = value as Record<string, unknown>;

    return (
            typeof candidate['token'] === 'string' &&
            candidate['tokenType'] === 'Bearer' &&
            typeof candidate['expiresAt'] === 'number' &&
            Array.isArray(candidate['roles']) &&
            candidate['roles'].every((role): role is UserRole => this.isUserRole(role)) &&
            this.isUserProfile(candidate['user'])
    );
  }

  private isUserProfile(value: unknown): value is UserProfile {
    if (typeof value !== 'object' || value === null) {
      return false;
    }

    const candidate = value as Record<string, unknown>;
    return (
      typeof candidate['id'] === 'number' &&
      typeof candidate['name'] === 'string' &&
      typeof candidate['surname'] === 'string' &&
      (typeof candidate['birthDate'] === 'string' || candidate['birthDate'] === null) &&
      typeof candidate['email'] === 'string' &&
      typeof candidate['present'] === 'boolean' &&
      typeof candidate['tutorialCompleted'] === 'boolean' &&
      Array.isArray(candidate['roles']) &&
      candidate['roles'].every((role): role is UserRole => this.isUserRole(role))
    );
  }

  private isUserRole(value: unknown): value is UserRole {
    return value === 'ADMIN' || value === 'USER' || value === 'VIP';
  }

  private storage(): Storage | null {
    if (typeof globalThis.localStorage === 'undefined') {
      return null;
    }

    return globalThis.localStorage;
  }
}
