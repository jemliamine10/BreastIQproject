import { Injectable, OnDestroy } from '@angular/core';
import { Subject, Observable } from 'rxjs';
import { StatusUpdatePayload, CriticalAlertPayload } from '../models/websocket-payloads';
import { DocumentEventDto } from '../models/document.dto';

/**
 * WebSocket service using native WebSocket with STOMP-like protocol.
 * Connects to /ws endpoint and subscribes to topics.
 * Falls back gracefully if server is unavailable.
 */
@Injectable({ providedIn: 'root' })
export class WebSocketService implements OnDestroy {
  private socket: WebSocket | null = null;
  private connected = false;
  private subscriptions = new Map<string, Subject<any>>();
  private reconnectTimer: any;
  private stompCounter = 0;

  private statusSubject = new Subject<StatusUpdatePayload>();
  private alertSubject = new Subject<CriticalAlertPayload>();
  private documentEventSubject = new Subject<DocumentEventDto>();

  /** Observable: patient status updates */
  statusUpdates$: Observable<StatusUpdatePayload> = this.statusSubject.asObservable();
  /** Observable: doctor alert notifications */
  alertNotifications$: Observable<CriticalAlertPayload> = this.alertSubject.asObservable();
  /** Observable: patient document events */
  documentEvents$: Observable<DocumentEventDto> = this.documentEventSubject.asObservable();

  /**
   * Connect to the WebSocket/SockJS endpoint.
   * Uses raw WebSocket for simplicity; backend SockJS also accepts native WS.
   */
  connect(): void {
    if (this.connected || this.socket) return;

    try {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const host = window.location.host;
      this.socket = new WebSocket(`${protocol}//${host}/ws/websocket`);

      this.socket.onopen = () => {
        this.connected = true;
        // STOMP CONNECT frame
        this.sendFrame('CONNECT', { 'accept-version': '1.2', 'heart-beat': '10000,10000' });
      };

      this.socket.onmessage = (event) => {
        this.handleFrame(event.data);
      };

      this.socket.onclose = () => {
        this.connected = false;
        this.socket = null;
        // Reconnect after 5s
        this.reconnectTimer = setTimeout(() => this.connect(), 5000);
      };

      this.socket.onerror = () => {
        // Will trigger onclose
      };
    } catch {
      console.warn('[WebSocket] Connection failed, will retry');
      this.reconnectTimer = setTimeout(() => this.connect(), 5000);
    }
  }

  /** Subscribe to patient status updates */
  subscribeToStatus(patientId: string): void {
    this.subscribeTo(`/topic/status/${patientId}`, (body: string) => {
      try {
        const payload: StatusUpdatePayload = JSON.parse(body);
        this.statusSubject.next(payload);
      } catch { /* ignore parse errors */ }
    });
  }

  /** Subscribe to doctor alert notifications */
  subscribeToAlerts(doctorId: string): void {
    this.subscribeTo(`/topic/alerts/${doctorId}`, (body: string) => {
      try {
        const payload: CriticalAlertPayload = JSON.parse(body);
        this.alertSubject.next(payload);
      } catch { /* ignore parse errors */ }
    });
  }

  /** Subscribe to patient documents updates */
  subscribeToPatientDocuments(patientId: string): void {
    this.subscribeTo(`/topic/patient/${patientId}`, (body: string) => {
      try {
        const payload = JSON.parse(body) as DocumentEventDto;
        if (payload?.type?.startsWith('DOCUMENT_')) {
          this.documentEventSubject.next(payload);
        }
      } catch { /* ignore parse errors */ }
    });
  }

  subscribeToDoctorDocuments(doctorId: string): void {
    this.subscribeTo(`/topic/doctor/${doctorId}`, (body: string) => {
      try {
        const payload = JSON.parse(body) as DocumentEventDto;
        if (payload?.type?.startsWith('DOCUMENT_')) {
          this.documentEventSubject.next(payload);
        }
      } catch { /* ignore parse errors */ }
    });
  }

  disconnect(): void {
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    if (this.socket && this.connected) {
      this.sendFrame('DISCONNECT', {});
      this.socket.close();
    }
    this.socket = null;
    this.connected = false;
    this.subscriptions.clear();
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.statusSubject.complete();
    this.alertSubject.complete();
    this.documentEventSubject.complete();
  }

  // ── STOMP protocol helpers ──

  subscribeTo(destination: string, callback: (body: string) => void): void {
    if (!this.subscriptions.has(destination)) {
      const sub = new Subject<string>();
      sub.subscribe(callback);
      this.subscriptions.set(destination, sub);

      if (this.connected) {
        this.sendStompSubscribe(destination);
      }
    }
  }

  private sendStompSubscribe(destination: string): void {
    const id = `sub-${this.stompCounter++}`;
    this.sendFrame('SUBSCRIBE', { id, destination });
  }

  private sendFrame(command: string, headers: Record<string, string>, body = ''): void {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return;
    let frame = command + '\n';
    Object.entries(headers).forEach(([k, v]) => { frame += `${k}:${v}\n`; });
    frame += '\n' + body + '\0';
    this.socket.send(frame);
  }

  private handleFrame(data: string): void {
    if (typeof data !== 'string') return;
    const lines = data.split('\n');
    const command = lines[0];

    if (command === 'CONNECTED') {
      // Re-subscribe to all topics
      this.subscriptions.forEach((_, dest) => this.sendStompSubscribe(dest));
      return;
    }

    if (command === 'MESSAGE') {
      const headers: Record<string, string> = {};
      let i = 1;
      while (i < lines.length && lines[i] !== '') {
        const [key, ...val] = lines[i].split(':');
        headers[key] = val.join(':');
        i++;
      }
      // Body is everything after empty line
      const body = lines.slice(i + 1).join('\n').replace(/\0$/, '');
      const dest = headers['destination'];
      if (dest && this.subscriptions.has(dest)) {
        this.subscriptions.get(dest)!.next(body);
      }
    }
  }
  /**
   * Send a STOMP message to a destination.
   */
  sendStompMessage(destination: string, body: string): void {
    this.sendFrame('SEND', { destination }, body);
  }
}
