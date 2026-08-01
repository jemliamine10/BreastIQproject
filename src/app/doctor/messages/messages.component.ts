import { Component, OnInit, OnDestroy, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { Router } from '@angular/router';
import { ChatMessageDto, ConversationDto } from '../../models/chat.models';
import { ChatService } from '../../services/chat.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-doctor-messages',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './messages.component.html',
  styleUrl: './messages.component.css'
})
export class MessagesComponent implements OnInit, OnDestroy, AfterViewChecked {

  @ViewChild('scrollMe') private myScrollContainer!: ElementRef;

  conversations: ConversationDto[] = [];
  activeConv: ConversationDto | null = null;
  messages: ChatMessageDto[] = [];

  currentUserId = '';
  newMessageText = '';
  searchQuery = '';
  loading = false;
  filterMode: 'all' | 'unread' | 'online' = 'all';

  private subs: Subscription[] = [];

  constructor(
    private chatService: ChatService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const user = this.authService.currentUser;
    if (user?.id) {
      this.currentUserId = user.id;
      this.chatService.initializeWebSocket(this.currentUserId);
      this.loadConversations();

      // Real-time: new message
      this.subs.push(this.chatService.newMessage$.subscribe(msg => {
        if (this.activeConv && msg.senderUserId === this.activeConv.partnerUserId) {
          this.messages.push(msg);
          this.chatService.markAsRead(this.currentUserId, this.activeConv.partnerUserId).subscribe();
        }
        this.loadConversations();
      }));

      // Real-time: read receipt
      this.subs.push(this.chatService.readReceipt$.subscribe(receipt => {
        if (this.activeConv && receipt.fromUserId === this.activeConv.partnerUserId) {
          this.messages.forEach(m => {
            if (m.senderUserId === this.currentUserId) m.status = 'READ';
          });
        }
      }));

      // Real-time: delivery receipt
      this.subs.push(this.chatService.deliveryReceipt$.subscribe(receipt => {
        if (this.activeConv && receipt.fromUserId === this.activeConv.partnerUserId) {
          this.messages.forEach(m => {
            if (m.senderUserId === this.currentUserId && m.status === 'SENT') m.status = 'DELIVERED';
          });
        }
      }));

      // Real-time: presence
      this.subs.push(this.chatService.presence$.subscribe(event => {
        const conv = this.conversations.find(c => c.partnerUserId === event.userId);
        if (conv) {
          conv.online = event.online;
          if (event.lastSeen) conv.lastSeen = event.lastSeen;
        }
      }));
    }
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
    if (this.currentUserId) this.chatService.sendOffline(this.currentUserId);
  }

  // ── Data Loading ──

  loadConversations(): void {
    this.chatService.getConversations(this.currentUserId).subscribe(convs => {
      this.conversations = convs;
      convs.forEach(c => this.chatService.subscribeToPresence(c.partnerUserId));
    });
  }

  selectConversation(conv: ConversationDto): void {
    this.activeConv = conv;
    this.loading = true;
    this.chatService.getMessages(this.currentUserId, conv.partnerUserId).subscribe(msgs => {
      this.messages = msgs;
      this.loading = false;
      conv.unreadCount = 0;
      this.chatService.markAsRead(this.currentUserId, conv.partnerUserId).subscribe();
    });
  }

  // ── Send ──

  sendMessage(): void {
    if (!this.newMessageText.trim() || !this.activeConv) return;

    this.chatService.sendMessage({
      senderUserId: this.currentUserId,
      recipientUserId: this.activeConv.partnerUserId,
      content: this.newMessageText,
      messageType: 'TEXT'
    }).subscribe(msg => {
      this.messages.push(msg);
      this.newMessageText = '';
      this.loadConversations();
    });
  }

  // ── Filtering ──

  get filteredConversations(): ConversationDto[] {
    let convs = this.conversations;
    if (this.filterMode === 'unread') convs = convs.filter(c => c.unreadCount > 0);
    if (this.filterMode === 'online') convs = convs.filter(c => c.online);
    if (!this.searchQuery) return convs;
    const q = this.searchQuery.toLowerCase();
    return convs.filter(c => c.partnerName.toLowerCase().includes(q));
  }

  get totalUnread(): number {
    return this.conversations.reduce((s, c) => s + c.unreadCount, 0);
  }

  get onlineCount(): number {
    return this.conversations.filter(c => c.online).length;
  }

  // ── Helpers ──

  getInitials(name: string): string {
    return name.split(' ').map(p => p[0]).join('').toUpperCase().substring(0, 2);
  }

  formatTime(isoString?: string): string {
    if (!isoString) return '';
    const d = new Date(isoString);
    return d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }

  formatLastSeen(conv: ConversationDto): string {
    if (conv.online) return 'En ligne';
    if (!conv.lastSeen) return 'Hors ligne';
    const d = new Date(conv.lastSeen);
    const now = new Date();
    const diff = Math.floor((now.getTime() - d.getTime()) / 60000);
    if (diff < 1) return 'Vu à l\'instant';
    if (diff < 60) return `Vu il y a ${diff}min`;
    if (diff < 1440) return `Vu il y a ${Math.floor(diff / 60)}h`;
    return `Vu le ${d.toLocaleDateString('fr-FR')}`;
  }

  isNewGroup(i: number): boolean {
    if (i === 0) return true;
    const curr = this.messages[i];
    const prev = this.messages[i - 1];
    if (curr.senderUserId !== prev.senderUserId) return true;
    const diff = new Date(curr.sentAt).getTime() - new Date(prev.sentAt).getTime();
    return diff > 5 * 60 * 1000;
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'READ': return '✓✓';
      case 'DELIVERED': return '✓✓';
      case 'SENT': return '✓';
      default: return '✓';
    }
  }

  getStatusClass(status: string): string {
    return status === 'READ' ? 'read' : '';
  }

  private scrollToBottom(): void {
    try {
      if (this.myScrollContainer) {
        this.myScrollContainer.nativeElement.scrollTop = this.myScrollContainer.nativeElement.scrollHeight;
      }
    } catch { /* ignore */ }
  }

  // Quick Actions Navigation
  goToPatients(): void {
    if (!this.activeConv) return;
    this.router.navigate(['/doctor/patients'], { 
      queryParams: { userId: this.activeConv.partnerUserId } 
    });
  }

  goToTreatment(): void {
    if (!this.activeConv) return;
    this.router.navigate(['/doctor/documents'], { 
      queryParams: { userId: this.activeConv.partnerUserId } 
    });
  }

  goToSchedule(): void {
    if (!this.activeConv) return;
    // Assuming schedule shows patient related appointments if needed
    this.router.navigate(['/doctor/schedule'], {
      queryParams: { userId: this.activeConv.partnerUserId }
    });
  }
}
