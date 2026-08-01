package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.entity.Alert;
import com.breastcancer.breastcancerbackend.entity.PatientProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Real-time notification service using WebSocket (STOMP).
 *
 * Pushes alerts to:
 * - /topic/alerts/{doctorId} — for doctor dashboard
 * - /topic/alerts/patient/{patientId} — for patient app (optional)
 */
@Service
public class NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Push an alert notification to the assigned doctor's WebSocket channel.
     */
    public void pushAlertToDoctor(UUID doctorProfileId, PatientProfile patient, Alert alert) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "CRITICAL_ALERT");
        payload.put("alertId", alert.getId());
        payload.put("patientId", patient.getId());
        payload.put("severity", alert.getSeverity().name());
        payload.put("alertType", alert.getAlertType().name());
        payload.put("message", alert.getMessage());
        payload.put("timestamp", Instant.now().toString());

        if (patient.getUser() != null) {
            payload.put("patientName", patient.getUser().getFirstName() + " " + patient.getUser().getLastName());
        }

        String destination = "/topic/alerts/" + doctorProfileId;
        messagingTemplate.convertAndSend(destination, payload);

        LOG.info("WebSocket notification sent to {} : [{}] {}", destination, alert.getSeverity(), alert.getAlertType());
    }

    /**
     * Push a status change notification to the patient.
     */
    public void pushStatusUpdate(UUID patientId, String status, int healthScore) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "STATUS_UPDATE");
        payload.put("patientId", patientId);
        payload.put("status", status);
        payload.put("healthScore", healthScore);
        payload.put("timestamp", Instant.now().toString());

        String destination = "/topic/status/" + patientId;
        messagingTemplate.convertAndSend(destination, payload);

        LOG.info("Status update sent to {} : status={}, score={}", destination, status, healthScore);
    }
}
