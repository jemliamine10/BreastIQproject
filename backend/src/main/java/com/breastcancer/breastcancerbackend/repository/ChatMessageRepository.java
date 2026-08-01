package com.breastcancer.breastcancerbackend.repository;

import com.breastcancer.breastcancerbackend.entity.ChatMessage;
import com.breastcancer.breastcancerbackend.entity.ChatMessage.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Get paginated conversation between two users (both directions), ordered by sentAt desc.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.deleted = false " +
           "AND ((m.sender.id = :u1 AND m.recipient.id = :u2) " +
           "  OR (m.sender.id = :u2 AND m.recipient.id = :u1)) " +
           "ORDER BY m.sentAt DESC")
    Page<ChatMessage> findConversation(@Param("u1") UUID user1, @Param("u2") UUID user2, Pageable pageable);

    /**
     * Count unread messages where userId is the recipient.
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.recipient.id = :userId " +
           "AND m.status <> :readStatus AND m.deleted = false")
    long countUnreadByRecipient(@Param("userId") UUID userId, @Param("readStatus") MessageStatus readStatus);

    /**
     * Count unread messages from a specific sender to a specific recipient.
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.sender.id = :senderId " +
           "AND m.recipient.id = :recipientId AND m.status <> :readStatus AND m.deleted = false")
    long countUnreadFromSender(@Param("senderId") UUID senderId, @Param("recipientId") UUID recipientId, @Param("readStatus") MessageStatus readStatus);

    /**
     * Get distinct partner user IDs for a given user (all people they've chatted with).
     */
    @Query("SELECT DISTINCT CASE WHEN m.sender.id = :userId THEN m.recipient.id ELSE m.sender.id END " +
           "FROM ChatMessage m WHERE (m.sender.id = :userId OR m.recipient.id = :userId) AND m.deleted = false")
    List<UUID> findDistinctPartnerIds(@Param("userId") UUID userId);

    /**
     * Mark all messages from sender to recipient as DELIVERED.
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.status = :deliveredStatus, m.deliveredAt = CURRENT_TIMESTAMP " +
           "WHERE m.sender.id = :senderId AND m.recipient.id = :recipientId " +
           "AND m.status = :sentStatus AND m.deleted = false")
    int markAsDelivered(@Param("senderId") UUID senderId, @Param("recipientId") UUID recipientId,
                        @Param("sentStatus") MessageStatus sentStatus, @Param("deliveredStatus") MessageStatus deliveredStatus);

    /**
     * Mark all messages from sender to recipient as READ.
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.status = :readStatus, m.readAt = CURRENT_TIMESTAMP " +
           "WHERE m.sender.id = :senderId AND m.recipient.id = :recipientId " +
           "AND m.status <> :readStatus AND m.deleted = false")
    int markAsRead(@Param("senderId") UUID senderId, @Param("recipientId") UUID recipientId,
                   @Param("readStatus") MessageStatus readStatus);

    /**
     * Find the last message in a conversation between two users.
     * Uses Pageable with size=1 instead of LIMIT (not valid JPQL).
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.deleted = false " +
           "AND ((m.sender.id = :u1 AND m.recipient.id = :u2) " +
           "  OR (m.sender.id = :u2 AND m.recipient.id = :u1)) " +
           "ORDER BY m.sentAt DESC")
    Page<ChatMessage> findLastMessages(@Param("u1") UUID user1, @Param("u2") UUID user2, Pageable pageable);
}
