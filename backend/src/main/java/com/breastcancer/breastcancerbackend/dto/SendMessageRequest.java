package com.breastcancer.breastcancerbackend.dto;

import java.util.UUID;

public class SendMessageRequest {
    private UUID senderUserId;
    private UUID recipientUserId;
    private String content;
    private String messageType; // TEXT or DOCUMENT
    private UUID documentId;    // nullable, only for DOCUMENT type

    // ===== Getters & Setters =====
    public UUID getSenderUserId() { return senderUserId; }
    public void setSenderUserId(UUID senderUserId) { this.senderUserId = senderUserId; }

    public UUID getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(UUID recipientUserId) { this.recipientUserId = recipientUserId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
}
