package com.breastcancer.breastcancerbackend.dto;

import java.time.Instant;
import java.util.UUID;

public class ConversationDto {
    private UUID partnerUserId;
    private String partnerName;
    private String partnerPhotoUrl;
    private String partnerSpeciality;  // doctor speciality or "Patient"
    private String partnerRole;       // DOCTOR or PATIENT
    private String lastMessage;
    private Instant lastMessageAt;
    private long unreadCount;
    private boolean online;
    private Instant lastSeen;

    // ===== Getters & Setters =====
    public UUID getPartnerUserId() { return partnerUserId; }
    public void setPartnerUserId(UUID partnerUserId) { this.partnerUserId = partnerUserId; }

    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }

    public String getPartnerPhotoUrl() { return partnerPhotoUrl; }
    public void setPartnerPhotoUrl(String partnerPhotoUrl) { this.partnerPhotoUrl = partnerPhotoUrl; }

    public String getPartnerSpeciality() { return partnerSpeciality; }
    public void setPartnerSpeciality(String partnerSpeciality) { this.partnerSpeciality = partnerSpeciality; }

    public String getPartnerRole() { return partnerRole; }
    public void setPartnerRole(String partnerRole) { this.partnerRole = partnerRole; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public Instant getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Instant lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(long unreadCount) { this.unreadCount = unreadCount; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }
}
