import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, BehaviorSubject, interval, Subscription } from 'rxjs';
import { ChatMessageDto, ConversationDto, SendMessageRequest, PresenceEvent } from '../models/chat.models';
import { WebSocketService } from './websocket.service';

@Injectable({ providedIn: 'root' })
export class ChatService implements OnDestroy {

  private readonly API = '/api/chat';

  // Real-time streams
  private newMessageSubject = new Subject<ChatMessageDto>();
  private deliveryReceiptSubject = new Subject<{ fromUserId: string; partnerUserId: string }>();
  private readReceiptSubject = new Subject<{ fromUserId: string; partnerUserId: string }>();
  private presenceSubject = new Subject<PresenceEvent>();
  private typingSubject = new Subject<{ userId: string; typing: boolean }>();
  private unreadCountSubject = new BehaviorSubject<number>(0);

  newMessage$ = this.newMessageSubject.asObservable();
  deliveryReceipt$ = this.deliveryReceiptSubject.asObservable();
  readReceipt$ = this.readReceiptSubject.asObservable();
  presence$ = this.presenceSubject.asObservable();
  typing$ = this.typingSubject.asObservable();
  unreadCount$ = this.unreadCountSubject.asObservable();

  private heartbeatSub?: Subscription;
  private chatSubscribed = false;

  constructor(
    private http: HttpClient,
    private wsService: WebSocketService
  ) {}

  // ==========================================
  // REST API Methods
  // ==========================================

  sendMessage(request: SendMessageRequest): Observable<ChatMessageDto> {
    return this.http.post<ChatMessageDto>(`${this.API}/send`, request);
  }

  getConversations(userId: string): Observable<ConversationDto[]> {
    return this.http.get<ConversationDto[]>(`${this.API}/conversations/${userId}`);
  }

  getMessages(userId: string, partnerId: string, page = 0, size = 50): Observable<ChatMessageDto[]> {
    return this.http.get<ChatMessageDto[]>(`${this.API}/messages/${userId}/${partnerId}?page=${page}&size=${size}`);
  }

  markAsRead(userId: string, partnerId: string): Observable<void> {
    return this.http.put<void>(`${this.API}/read/${userId}/${partnerId}`, {});
  }

  fetchUnreadCount(userId: string): void {
    this.http.get<{ count: number }>(`${this.API}/unread-count/${userId}`).subscribe(res => {
      this.unreadCountSubject.next(res.count);
    });
  }

  // ==========================================
  // WebSocket Subscriptions
  // ==========================================

  /**
   * Subscribe to real-time chat events for the current user.
   * Should be called once after login.
   */
  initializeWebSocket(userId: string): void {
    if (this.chatSubscribed) return;
    this.chatSubscribed = true;

    // Connect to WS
    this.wsService.connect();

    // Subscribe to chat topic
    this.wsService.subscribeTo(`/topic/chat/${userId}`, (body: string) => {
      try {
        const payload = JSON.parse(body);

        if (payload.type === 'DELIVERY_RECEIPT') {
          this.deliveryReceiptSubject.next(payload);
        } else if (payload.type === 'READ_RECEIPT') {
          this.readReceiptSubject.next(payload);
        } else if (payload.id && payload.senderUserId) {
          // It's a ChatMessageDto
          this.newMessageSubject.next(payload as ChatMessageDto);
        }
      } catch { /* ignore parse errors */ }
    });

    // Start presence heartbeat every 30 seconds
    this.sendPresenceAction(userId, 'online');
    this.heartbeatSub = interval(30000).subscribe(() => {
      this.sendPresenceAction(userId, 'heartbeat');
    });

    // Fetch initial unread
    this.fetchUnreadCount(userId);
  }

  /**
   * Subscribe to presence updates for a specific user (partner).
   */
  subscribeToPresence(partnerUserId: string): void {
    this.wsService.subscribeTo(`/topic/presence/${partnerUserId}`, (body: string) => {
      try {
        const event: PresenceEvent = JSON.parse(body);
        this.presenceSubject.next(event);
      } catch { /* ignore */ }
    });
  }

  // ==========================================
  // STOMP Outbound
  // ==========================================

  private sendPresenceAction(userId: string, action: string): void {
    this.wsService.sendStompMessage('/app/chat.presence', JSON.stringify({ userId, action }));
  }

  sendTypingIndicator(userId: string, recipientId: string, typing: boolean): void {
    this.wsService.sendStompMessage('/app/chat.typing', JSON.stringify({ userId, recipientId, typing }));
  }

  sendOffline(userId: string): void {
    this.sendPresenceAction(userId, 'offline');
  }

  ngOnDestroy(): void {
    this.heartbeatSub?.unsubscribe();
    this.newMessageSubject.complete();
    this.deliveryReceiptSubject.complete();
    this.readReceiptSubject.complete();
    this.presenceSubject.complete();
    this.typingSubject.complete();
  }
}
