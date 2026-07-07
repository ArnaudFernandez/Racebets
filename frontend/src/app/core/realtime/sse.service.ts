import { computed, Injectable, signal } from '@angular/core';

import { RaceBettingUpdate } from '../../features/betting/models/betting.model';

@Injectable({ providedIn: 'root' })
export class SseService {
  private readonly eventSource = signal<EventSource | null>(null);
  private readonly updates = signal<readonly RaceBettingUpdate[]>([]);
  private reconnectTimer: number | null = null;
  private manuallyDisconnected = false;

  readonly bettingUpdates = computed(() => this.updates());

  connect(): void {
    if (this.eventSource()) return;

    this.manuallyDisconnected = false;
    this.clearReconnectTimer();

    const es = new EventSource('/api/realtime/race-betting/stream');

    es.onmessage = (event) => {
      try {
        const data: RaceBettingUpdate[] = JSON.parse(event.data);
        this.updates.set(data);
      } catch {
        console.error('Failed to parse SSE message', event.data);
      }
    };

    es.onerror = () => {
      es.close();
      this.eventSource.set(null);
      if (!this.manuallyDisconnected) {
        this.reconnectTimer = window.setTimeout(() => this.connect(), 3000);
      }
    };

    this.eventSource.set(es);
  }

  disconnect(): void {
    this.manuallyDisconnected = true;
    this.clearReconnectTimer();
    this.eventSource()?.close();
    this.eventSource.set(null);
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer !== null) {
      window.clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }
}
