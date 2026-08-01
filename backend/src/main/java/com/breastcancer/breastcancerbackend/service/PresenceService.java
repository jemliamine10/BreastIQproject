package com.breastcancer.breastcancerbackend.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks user online/offline presence using an in-memory map.
 * Broadcasts presence changes via WebSocket.
 */
@Service
public class PresenceService {

    private final ConcurrentHashMap<UUID, Instant> onlineUsers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Instant> lastSeenMap = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Mark user as online and broadcast presence.
     */
    public void setOnline(UUID userId) {
        onlineUsers.put(userId, Instant.now());
        broadcastPresence(userId, true, null);
    }

    /**
     * Mark user as offline with last seen timestamp and broadcast.
     */
    public void setOffline(UUID userId) {
        onlineUsers.remove(userId);
        Instant now = Instant.now();
        lastSeenMap.put(userId, now);
        broadcastPresence(userId, false, now);
    }

    /**
     * Heartbeat: refresh the user's online timestamp.
     */
    public void heartbeat(UUID userId) {
        onlineUsers.put(userId, Instant.now());
    }

    /**
     * Check if a user is currently online.
     * A user is considered offline if no heartbeat for > 60 seconds.
     */
    public boolean isOnline(UUID userId) {
        Instant lastBeat = onlineUsers.get(userId);
        if (lastBeat == null) return false;
        return Instant.now().minusSeconds(60).isBefore(lastBeat);
    }

    /**
     * Get last seen timestamp for a user. Returns null if never tracked.
     */
    public Instant getLastSeen(UUID userId) {
        if (isOnline(userId)) return Instant.now();
        return lastSeenMap.get(userId);
    }

    private void broadcastPresence(UUID userId, boolean online, Instant lastSeen) {
        Map<String, Object> payload = Map.of(
                "userId", userId.toString(),
                "online", online,
                "lastSeen", lastSeen != null ? lastSeen.toString() : ""
        );
        messagingTemplate.convertAndSend("/topic/presence/" + userId, payload);
    }
}
