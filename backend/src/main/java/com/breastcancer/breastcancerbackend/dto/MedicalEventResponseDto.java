package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.MedicalEvent;
import java.time.Instant;
import java.util.UUID;

public class MedicalEventResponseDto {
    private UUID id;
    private UUID patientId;
    private MedicalEvent.EventType eventType;
    private String title;
    private String description;
    private String severity;
    private UUID referenceId;
    private String referenceType;
    private Instant eventDate;
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID pid) { this.patientId = pid; }

    public MedicalEvent.EventType getEventType() { return eventType; }
    public void setEventType(MedicalEvent.EventType et) { this.eventType = et; }

    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }

    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }

    public String getSeverity() { return severity; }
    public void setSeverity(String s) { this.severity = s; }

    public UUID getReferenceId() { return referenceId; }
    public void setReferenceId(UUID ri) { this.referenceId = ri; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String rt) { this.referenceType = rt; }

    public Instant getEventDate() { return eventDate; }
    public void setEventDate(Instant ed) { this.eventDate = ed; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant ca) { this.createdAt = ca; }
}
