package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.ChatMessageDto;
import com.breastcancer.breastcancerbackend.dto.ConversationDto;
import com.breastcancer.breastcancerbackend.dto.SendMessageRequest;
import com.breastcancer.breastcancerbackend.entity.ChatMessage;
import com.breastcancer.breastcancerbackend.entity.MedicalDocument;
import com.breastcancer.breastcancerbackend.entity.User;
import com.breastcancer.breastcancerbackend.repository.ChatMessageRepository;
import com.breastcancer.breastcancerbackend.repository.MedicalDocumentRepository;
import com.breastcancer.breastcancerbackend.repository.UserRepository;
import com.breastcancer.breastcancerbackend.repository.PatientDoctorLinkRepository;
import com.breastcancer.breastcancerbackend.entity.PatientDoctorLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatMessageRepository chatRepo;
    private final UserRepository userRepo;
    private final MedicalDocumentRepository documentRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;
    private final PatientDoctorLinkRepository linkRepo;

    public ChatService(ChatMessageRepository chatRepo,
                       UserRepository userRepo,
                       MedicalDocumentRepository documentRepo,
                       SimpMessagingTemplate messagingTemplate,
                       PresenceService presenceService,
                       PatientDoctorLinkRepository linkRepo) {
        this.chatRepo = chatRepo;
        this.userRepo = userRepo;
        this.documentRepo = documentRepo;
        this.messagingTemplate = messagingTemplate;
        this.presenceService = presenceService;
        this.linkRepo = linkRepo;
    }

    // ==========================================
    // Send Message
    // ==========================================

    @Transactional
    public ChatMessageDto sendMessage(SendMessageRequest request) {
        User sender = userRepo.findById(request.getSenderUserId())
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User recipient = userRepo.findById(request.getRecipientUserId())
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        ChatMessage msg = new ChatMessage();
        msg.setSender(sender);
        msg.setRecipient(recipient);
        msg.setContent(request.getContent());
        msg.setStatus(ChatMessage.MessageStatus.SENT);
        msg.setSentAt(Instant.now());

        // Handle message type
        String type = request.getMessageType();
        if ("DOCUMENT".equalsIgnoreCase(type) && request.getDocumentId() != null) {
            msg.setMessageType(ChatMessage.MessageType.DOCUMENT);
            MedicalDocument doc = documentRepo.findById(request.getDocumentId()).orElse(null);
            msg.setDocument(doc);
        } else {
            msg.setMessageType(ChatMessage.MessageType.TEXT);
        }

        msg = chatRepo.save(msg);

        ChatMessageDto dto = toDto(msg);

        // Broadcast to recipient via WebSocket
        messagingTemplate.convertAndSend(
                "/topic/chat/" + recipient.getId(), dto
        );

        return dto;
    }

    // ==========================================
    // Get Conversations List
    // ==========================================

    @Transactional(readOnly = true)
    public List<ConversationDto> getConversations(UUID userId) {
        Set<UUID> partnerIdsSet = new HashSet<>(chatRepo.findDistinctPartnerIds(userId));

        // Incorporate allowed contacts from PatientDoctorLink
        User currentUser = userRepo.findById(userId).orElse(null);
        if (currentUser != null) {
            if (currentUser.getDoctorProfile() != null) {
                List<PatientDoctorLink> doctorLinks = linkRepo.findByDoctor_IdAndStatusOrderByActivatedAtDesc(currentUser.getDoctorProfile().getId(), PatientDoctorLink.Status.ACTIVE);
                for (PatientDoctorLink link : doctorLinks) {
                    if (link.getPatient() != null && link.getPatient().getUser() != null) {
                        partnerIdsSet.add(link.getPatient().getUser().getId());
                    }
                }
            }
            if (currentUser.getPatientProfile() != null) {
                List<PatientDoctorLink> patientLinks = linkRepo.findByPatient_IdAndStatus(currentUser.getPatientProfile().getId(), PatientDoctorLink.Status.ACTIVE);
                for (PatientDoctorLink link : patientLinks) {
                    if (link.getDoctor() != null && link.getDoctor().getUser() != null) {
                        partnerIdsSet.add(link.getDoctor().getUser().getId());
                    }
                }
            }
        }

        return partnerIdsSet.stream().map(partnerId -> {
            User partner = userRepo.findById(partnerId).orElse(null);
            if (partner == null) return null;

            ConversationDto conv = new ConversationDto();
            conv.setPartnerUserId(partner.getId());
            conv.setPartnerName(partner.getFirstName() + " " + partner.getLastName());
            conv.setPartnerPhotoUrl(partner.getProfilePhotoUrl());
            conv.setPartnerRole(partner.getRole().name());

            // Speciality for doctors
            if (partner.getDoctorProfile() != null) {
                conv.setPartnerSpeciality(partner.getDoctorProfile().getSpeciality());
            } else {
                conv.setPartnerSpeciality("Patient");
            }

            // Last message
            Page<ChatMessage> lastPage = chatRepo.findLastMessages(userId, partnerId, PageRequest.of(0, 1));
            if (!lastPage.isEmpty()) {
                ChatMessage lastMsg = lastPage.getContent().get(0);
                conv.setLastMessage(lastMsg.getContent());
                conv.setLastMessageAt(lastMsg.getSentAt());
            }

            // Unread count (messages FROM partner TO me)
            conv.setUnreadCount(chatRepo.countUnreadFromSender(partnerId, userId, ChatMessage.MessageStatus.READ));

            // Presence
            conv.setOnline(presenceService.isOnline(partnerId));
            conv.setLastSeen(presenceService.getLastSeen(partnerId));

            return conv;
        })
        .filter(Objects::nonNull)
        .sorted((a, b) -> {
            if (a.getLastMessageAt() == null && b.getLastMessageAt() == null) return 0;
            if (a.getLastMessageAt() == null) return 1;
            if (b.getLastMessageAt() == null) return -1;
            return b.getLastMessageAt().compareTo(a.getLastMessageAt());
        })
        .collect(Collectors.toList());
    }

    // ==========================================
    // Get Messages (Paginated)
    // ==========================================

    @Transactional
    public List<ChatMessageDto> getMessages(UUID userId, UUID partnerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> messagePage = chatRepo.findConversation(userId, partnerId, pageable);

        // Auto-mark as DELIVERED when fetched
        chatRepo.markAsDelivered(partnerId, userId, ChatMessage.MessageStatus.SENT, ChatMessage.MessageStatus.DELIVERED);

        // Broadcast delivery receipt to partner
        Map<String, Object> deliveryReceipt = Map.of(
                "type", "DELIVERY_RECEIPT",
                "fromUserId", userId.toString(),
                "partnerUserId", partnerId.toString()
        );
        messagingTemplate.convertAndSend("/topic/chat/" + partnerId, deliveryReceipt);

        List<ChatMessageDto> messages = messagePage.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        // Reverse so oldest first for display
        Collections.reverse(messages);
        return messages;
    }

    // ==========================================
    // Mark As Read
    // ==========================================

    @Transactional
    public void markAsRead(UUID userId, UUID partnerId) {
        int updated = chatRepo.markAsRead(partnerId, userId, ChatMessage.MessageStatus.READ);

        if (updated > 0) {
            // Broadcast read receipt to partner
            Map<String, Object> readReceipt = Map.of(
                    "type", "READ_RECEIPT",
                    "fromUserId", userId.toString(),
                    "partnerUserId", partnerId.toString()
            );
            messagingTemplate.convertAndSend("/topic/chat/" + partnerId, readReceipt);
        }
    }

    // ==========================================
    // Unread Count
    // ==========================================

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return chatRepo.countUnreadByRecipient(userId, ChatMessage.MessageStatus.READ);
    }

    // ==========================================
    // Entity -> DTO mapper
    // ==========================================

    private ChatMessageDto toDto(ChatMessage msg) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(msg.getId());
        dto.setSenderUserId(msg.getSender().getId());
        dto.setRecipientUserId(msg.getRecipient().getId());
        dto.setSenderName(msg.getSender().getFirstName() + " " + msg.getSender().getLastName());
        dto.setSenderPhotoUrl(msg.getSender().getProfilePhotoUrl());
        dto.setContent(msg.getContent());
        dto.setMessageType(msg.getMessageType().name());
        dto.setStatus(msg.getStatus().name());
        dto.setSentAt(msg.getSentAt());
        dto.setDeliveredAt(msg.getDeliveredAt());
        dto.setReadAt(msg.getReadAt());

        if (msg.getDocument() != null) {
            dto.setDocumentId(msg.getDocument().getId());
            dto.setDocumentName(msg.getDocument().getName());
        }

        return dto;
    }
}
