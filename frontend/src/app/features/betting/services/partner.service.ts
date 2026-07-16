import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { PublicPartner } from '../models/partner.model';

@Injectable({providedIn: 'root'})
export class PartnerService {
  private readonly http = inject(HttpClient);

  findVisible(): Promise<readonly PublicPartner[]> {
    return firstValueFrom(this.http.get<readonly PublicPartner[]>('/api/partners'));
  }
}
