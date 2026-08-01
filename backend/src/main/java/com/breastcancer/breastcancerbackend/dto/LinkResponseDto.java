package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.PatientDoctorLink;

import java.time.Instant;
import java.util.UUID;

public class LinkResponseDto {

    private UUID id;
    private UUID patientProfileId;
    private UUID doctorProfileId;

    private PatientDoctorLink.Status status;
    private PatientDoctorLink.RequestedBy requestedBy;

    private String requestNote;

    private UUID decisionByUserId;
    private String rejectionReason;

    private Instant requestedAt;
    private Instant activatedAt;
    private Instant endedAt;
    private Instant lastUpdatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPatientProfileId() { return patientProfileId; }
    public void setPatientProfileId(UUID patientProfileId) { this.patientProfileId = patientProfileId; }

    public UUID getDoctorProfileId() { return doctorProfileId; }
    public void setDoctorProfileId(UUID doctorProfileId) { this.doctorProfileId = doctorProfileId; }

    public PatientDoctorLink.Status getStatus() { return status; }
    public void setStatus(PatientDoctorLink.Status status) { this.status = status; }

    public PatientDoctorLink.RequestedBy getRequestedBy() { return requestedBy; }
    public void setRequestedBy(PatientDoctorLink.RequestedBy requestedBy) { this.requestedBy = requestedBy; }

    public String getRequestNote() { return requestNote; }
    public void setRequestNote(String requestNote) { this.requestNote = requestNote; }

    public UUID getDecisionByUserId() { return decisionByUserId; }
    public void setDecisionByUserId(UUID decisionByUserId) { this.decisionByUserId = decisionByUserId; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }

    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

    public Instant getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(Instant lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
}
