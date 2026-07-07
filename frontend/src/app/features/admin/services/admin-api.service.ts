import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import {
  AdminUserRequest,
  AdminUserResponse,
  HorseAdminRequest,
  HorseAdminResponse,
  RaceAdminRequest,
  RaceAdminResponse,
  RaceEntryAdminRequest,
  RaceEntryAdminResponse
} from '../models/admin-api.model';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/admin';

  findUsers(): Promise<readonly AdminUserResponse[]> {
    return firstValueFrom(this.http.get<readonly AdminUserResponse[]>(`${this.baseUrl}/users`));
  }

  createUser(request: AdminUserRequest): Promise<AdminUserResponse> {
    return firstValueFrom(this.http.post<AdminUserResponse>(`${this.baseUrl}/users`, request));
  }

  updateUser(id: number, request: AdminUserRequest): Promise<AdminUserResponse> {
    return firstValueFrom(this.http.put<AdminUserResponse>(`${this.baseUrl}/users/${id}`, request));
  }

  deleteUser(id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${this.baseUrl}/users/${id}`));
  }

  findHorses(): Promise<readonly HorseAdminResponse[]> {
    return firstValueFrom(this.http.get<readonly HorseAdminResponse[]>(`${this.baseUrl}/horses`));
  }

  createHorse(request: HorseAdminRequest): Promise<HorseAdminResponse> {
    return firstValueFrom(this.http.post<HorseAdminResponse>(`${this.baseUrl}/horses`, request));
  }

  updateHorse(id: number, request: HorseAdminRequest): Promise<HorseAdminResponse> {
    return firstValueFrom(this.http.put<HorseAdminResponse>(`${this.baseUrl}/horses/${id}`, request));
  }

  deleteHorse(id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${this.baseUrl}/horses/${id}`));
  }

  findRaces(): Promise<readonly RaceAdminResponse[]> {
    return firstValueFrom(this.http.get<readonly RaceAdminResponse[]>(`${this.baseUrl}/races`));
  }

  createRace(request: RaceAdminRequest): Promise<RaceAdminResponse> {
    return firstValueFrom(this.http.post<RaceAdminResponse>(`${this.baseUrl}/races`, request));
  }

  updateRace(id: number, request: RaceAdminRequest): Promise<RaceAdminResponse> {
    return firstValueFrom(this.http.put<RaceAdminResponse>(`${this.baseUrl}/races/${id}`, request));
  }

  deleteRace(id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${this.baseUrl}/races/${id}`));
  }

  findRaceEntries(raceId: number | null = null): Promise<readonly RaceEntryAdminResponse[]> {
    const url = raceId === null ? `${this.baseUrl}/race-entries` : `${this.baseUrl}/race-entries?raceId=${raceId}`;

    return firstValueFrom(this.http.get<readonly RaceEntryAdminResponse[]>(url));
  }

  createRaceEntry(request: RaceEntryAdminRequest): Promise<RaceEntryAdminResponse> {
    return firstValueFrom(this.http.post<RaceEntryAdminResponse>(`${this.baseUrl}/race-entries`, request));
  }

  updateRaceEntry(id: number, request: RaceEntryAdminRequest): Promise<RaceEntryAdminResponse> {
    return firstValueFrom(this.http.put<RaceEntryAdminResponse>(`${this.baseUrl}/race-entries/${id}`, request));
  }

  deleteRaceEntry(id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${this.baseUrl}/race-entries/${id}`));
  }
}
