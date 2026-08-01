package com.breastcancer.breastcancerbackend.dto;

import java.time.Instant;
import java.util.UUID;

public class ChatMessageDto {
    private UUID id;
    private UUID senderUserId;
    private UUID recipientUserId;
    private String senderName;
    private String senderPhotoUrl;
    private String content;
    private String messageType; // TEXT, DOCUMENT
    private UUID documentId;
    private String documentName;
    private String status; // SENT, DELIVERED, READ
    private Instant sentAt;
    private Instant deliveredAt;
    private Instant readAt;

    // ===== Getters & Setters =====
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSenderUserId() { return senderUserId; }
    public void setSenderUserId(UUID senderUserId) { this.senderUserId = senderUserId; }

    public UUID getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(UUID recipientUserId) { this.recipientUserId = recipientUserId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderPhotoUrl() { return senderPhotoUrl; }
    public void setSenderPhotoUrl(String senderPhotoUrl) { this.senderPhotoUrl = senderPhotoUrl; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }

    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
}
