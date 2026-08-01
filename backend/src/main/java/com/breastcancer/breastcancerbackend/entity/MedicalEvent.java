package com.breastcancer.breastcancerbackend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "medical_events",
        indexes = {
                @Index(name = "idx_medevent_patient", columnList = "patient_id"),
                @Index(name = "idx_medevent_type", columnList = "event_type"),
                @Index(name = "idx_medevent_date", columnList = "event_date")
        }
)
public class MedicalEvent {

    public enum EventType {
        DIAGNOSIS,
        TREATMENT_START,
        TREATMENT_END,
        SESSION_COMPLETED,
        SESSION_MISSED,
        ALERT_GENERATED,
        TRACKER_ENTRY,
        APPOINTMENT,
        MEDICAL_NOTE,
        STATUS_CHANGE
    }

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_medevent_patient"))
    private PatientProfile patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "severity", length = 20)
    private String severity; // optional — used when event is alert-related

    @Column(name = "reference_id")
    private UUID referenceId; // FK to the entity that triggered this event

    @Column(name = "reference_type", length = 50)
    private String referenceType; // e.g. "TREATMENT", "ALERT", "TRACKER_ENTRY"

    @Column(name = "event_date", nullable = false)
    private Instant eventDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // ===== Getters & Setters =====
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PatientProfile getPatient() { return patient; }
    public void setPatient(PatientProfile patient) { this.patient = patient; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public UUID getReferenceId() { return referenceId; }
    public void setReferenceId(UUID referenceId) { this.referenceId = referenceId; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public Instant getEventDate() { return eventDate; }
    public void setEventDate(Instant eventDate) { this.eventDate = eventDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
