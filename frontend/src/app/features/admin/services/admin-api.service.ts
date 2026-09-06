import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { AppBrandingSettings } from '../../../core/branding/app-branding.model';
import {
  AdminUserRequest,
  AdminUserResponse,
  HorseAdminRequest,
  HorseAdminResponse,
  PartnerAdminResponse,
  RaceAdminRequest,
  RaceAdminResponse,
  RaceControl,
  RaceHistoryDetail,
  RaceHistorySummary,
  UserImportPreview,
  UserImportResult
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

  previewUserImport(file: File): Promise<UserImportPreview> {
    const data = new FormData();
    data.append('file', file);
    return firstValueFrom(
      this.http.post<UserImportPreview>(`${this.baseUrl}/users/import/preview`, data)
    );
  }

  confirmUserImport(file: File, preview: UserImportPreview): Promise<UserImportResult> {
    const data = new FormData();
    data.append('file', file);
    data.append('fileDigest', preview.fileDigest);
    data.append('planFingerprint', preview.planFingerprint);
    return firstValueFrom(
      this.http.post<UserImportResult>(`${this.baseUrl}/users/import/confirm`, data)
    );
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

  uploadRaceImage(id: number, image: File): Promise<RaceAdminResponse> {
    const data = new FormData();
    data.append('image', image);
    return firstValueFrom(this.http.post<RaceAdminResponse>(`${this.baseUrl}/races/${id}/image`, data));
  }

  deleteRace(id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${this.baseUrl}/races/${id}`));
  }

  getRaceControl(id: number): Promise<RaceControl> {
    return firstValueFrom(this.http.get<RaceControl>(`${this.baseUrl}/races/${id}/control`));
  }

  transitionRace(id: number, state: RaceControl['state']): Promise<RaceControl> {
    return firstValueFrom(this.http.post<RaceControl>(`${this.baseUrl}/races/${id}/state`, { state }));
  }

  publishRaceResult(id: number, orderedEntryIds: readonly number[]): Promise<RaceControl> {
    return firstValueFrom(this.http.post<RaceControl>(`${this.baseUrl}/races/${id}/result`, { orderedEntryIds }));
  }

  correctRaceResult(
    id: number,
    expectedOrderedEntryIds: readonly number[],
    orderedEntryIds: readonly number[]
  ): Promise<RaceHistoryDetail> {
    return firstValueFrom(
      this.http.put<RaceHistoryDetail>(`${this.baseUrl}/race-history/${id}/result`, {
        expectedOrderedEntryIds,
        orderedEntryIds
      })
    );
  }

  selectRaceRunners(id: number, horseIds: readonly number[]): Promise<RaceControl> {
    return firstValueFrom(this.http.put<RaceControl>(`${this.baseUrl}/races/${id}/runners`, { horseIds }));
  }

  clearRaceFromLive(id: number): Promise<RaceControl> {
    return firstValueFrom(this.http.post<RaceControl>(`${this.baseUrl}/races/${id}/clear-live`, null));
  }

  findRaceHistory(): Promise<readonly RaceHistorySummary[]> {
    return firstValueFrom(this.http.get<readonly RaceHistorySummary[]>(`${this.baseUrl}/race-history`));
  }

  getRaceHistoryDetail(id: number): Promise<RaceHistoryDetail> {
    return firstValueFrom(this.http.get<RaceHistoryDetail>(`${this.baseUrl}/race-history/${id}`));
  }

  findPartners(): Promise<readonly PartnerAdminResponse[]> {
    return firstValueFrom(this.http.get<readonly PartnerAdminResponse[]>(`${this.baseUrl}/partners`));
  }

  createPartner(formData: FormData): Promise<PartnerAdminResponse> {
    return firstValueFrom(this.http.post<PartnerAdminResponse>(`${this.baseUrl}/partners`, formData));
  }

  updatePartner(id: number, formData: FormData): Promise<PartnerAdminResponse> {
    return firstValueFrom(this.http.put<PartnerAdminResponse>(`${this.baseUrl}/partners/${id}`, formData));
  }

  deletePartner(id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${this.baseUrl}/partners/${id}`));
  }

  findBranding(): Promise<AppBrandingSettings> {
    return firstValueFrom(this.http.get<AppBrandingSettings>('/api/app/branding'));
  }

  updateBranding(formData: FormData): Promise<AppBrandingSettings> {
    return firstValueFrom(this.http.put<AppBrandingSettings>(`${this.baseUrl}/app/branding`, formData));
  }
}
