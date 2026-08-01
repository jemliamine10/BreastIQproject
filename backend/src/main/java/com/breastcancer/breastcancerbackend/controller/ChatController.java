package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.ChatMessageDto;
import com.breastcancer.breastcancerbackend.dto.ConversationDto;
import com.breastcancer.breastcancerbackend.dto.SendMessageRequest;
import com.breastcancer.breastcancerbackend.service.ChatService;
import com.breastcancer.breastcancerbackend.service.PresenceService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final PresenceService presenceService;

    public ChatController(ChatService chatService, PresenceService presenceService) {
        this.chatService = chatService;
        this.presenceService = presenceService;
    }

    // ==========================================
    // REST Endpoints
    // ==========================================

    /**
     * Send a new message (text or document).
     */
    @PostMapping("/send")
    public ChatMessageDto sendMessage(@RequestBody SendMessageRequest request) {
        return chatService.sendMessage(request);
    }

    /**
     * Get all conversations for a user.
     */
    @GetMapping("/conversations/{userId}")
    public List<ConversationDto> getConversations(@PathVariable UUID userId) {
        return chatService.getConversations(userId);
    }

    /**
     * Get paginated messages for a conversation.
     */
    @GetMapping("/messages/{userId}/{partnerId}")
    public List<ChatMessageDto> getMessages(
            @PathVariable UUID userId,
            @PathVariable UUID partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return chatService.getMessages(userId, partnerId, page, size);
    }

    /**
     * Mark all messages from partner as read.
     */
    @PutMapping("/read/{userId}/{partnerId}")
    public void markAsRead(@PathVariable UUID userId, @PathVariable UUID partnerId) {
        chatService.markAsRead(userId, partnerId);
    }

    /**
     * Get total unread message count for badge.
     */
    @GetMapping("/unread-count/{userId}")
    public Map<String, Long> getUnreadCount(@PathVariable UUID userId) {
        return Map.of("count", chatService.getUnreadCount(userId));
    }

    // ==========================================
    // STOMP Endpoints (WebSocket)
    // ==========================================

    /**
     * Presence heartbeat via STOMP.
     * Client sends to /app/chat.presence with {"userId": "..."}
     */
    @MessageMapping("/chat.presence")
    public void handlePresence(@Payload Map<String, String> payload) {
        String userId = payload.get("userId");
        String action = payload.getOrDefault("action", "heartbeat");

        if (userId == null) return;
        UUID uid = UUID.fromString(userId);

        switch (action) {
            case "online":
                presenceService.setOnline(uid);
                break;
            case "offline":
                presenceService.setOffline(uid);
                break;
            default:
                presenceService.heartbeat(uid);
        }
    }

    /**
     * Typing indicator via STOMP.
     * Client sends to /app/chat.typing with {"userId": "...", "recipientId": "...", "typing": true}
     * Server broadcasts to /topic/typing/{recipientId}
     */
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload Map<String, Object> payload) {
        String recipientId = (String) payload.get("recipientId");
        if (recipientId != null) {
            // This will be picked up by the SimpMessagingTemplate auto-routing
            // but we need to manually forward it
        }
    }
}
