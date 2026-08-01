export interface ChatMessageDto {
  id: string;
  senderUserId: string;
  recipientUserId: string;
  senderName: string;
  senderPhotoUrl?: string;
  content: string;
  messageType: 'TEXT' | 'DOCUMENT';
  documentId?: string;
  documentName?: string;
  status: 'SENT' | 'DELIVERED' | 'READ';
  sentAt: string;
  deliveredAt?: string;
  readAt?: string;
}

export interface ConversationDto {
  partnerUserId: string;
  partnerName: string;
  partnerPhotoUrl?: string;
  partnerSpeciality?: string;
  partnerRole: 'DOCTOR' | 'PATIENT';
  lastMessage?: string;
  lastMessageAt?: string;
  unreadCount: number;
  online: boolean;
  lastSeen?: string;
}

export interface SendMessageRequest {
  senderUserId: string;
  recipientUserId: string;
  content: string;
  messageType: 'TEXT' | 'DOCUMENT';
  documentId?: string;
}

export interface PresenceEvent {
  userId: string;
  online: boolean;
  lastSeen?: string;
}

export interface TypingEvent {
  userId: string;
  recipientId: string;
  typing: boolean;
}

export interface ChatNotification {
  type: 'NEW_MESSAGE' | 'DELIVERY_RECEIPT' | 'READ_RECEIPT';
  fromUserId?: string;
  partnerUserId?: string;
  message?: ChatMessageDto;
}
