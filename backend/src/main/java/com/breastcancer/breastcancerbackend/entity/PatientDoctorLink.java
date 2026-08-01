package com.breastcancer.breastcancerbackend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "patient_doctor_links",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_patient_doctor_pair",
                        columnNames = {"patient_id", "doctor_id"}
                )
        },
        indexes = {
                @Index(name = "idx_link_patient", columnList = "patient_id"),
                @Index(name = "idx_link_doctor", columnList = "doctor_id"),
                @Index(name = "idx_link_status", columnList = "status")
        }
)
public class PatientDoctorLink {

    public enum Status {
        REQUESTED,   // demande envoyée
        ACTIVE,      // acceptée -> RDV autorisés
        REJECTED,
        BLOCKED,
        ENDED
    }

    public enum RequestedBy {
        PATIENT,
        DOCTOR
    }

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_link_patient"))
    private PatientProfile patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_link_doctor"))
    private DoctorProfile doctor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.REQUESTED;

    // Qui a initié la connexion ?
    @Enumerated(EnumType.STRING)
    @Column(name = "requested_by", nullable = false, length = 20)
    private RequestedBy requestedBy;

    // Message optionnel de demande
    @Column(name = "request_note", length = 1000)
    private String requestNote;

    // Décision
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decision_by_user_id",
            foreignKey = @ForeignKey(name = "fk_link_decision_user"))
    private User decisionByUser;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    // Dates importantes
    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt = Instant.now();

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.lastUpdatedAt = Instant.now();
    }

    // ===== Helpers =====
    @Transient
    public boolean isActive() {
        return this.status == Status.ACTIVE;
    }

    // ===== Getters & Setters =====

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PatientProfile getPatient() { return patient; }
    public void setPatient(PatientProfile patient) { this.patient = patient; }

    public DoctorProfile getDoctor() { return doctor; }
    public void setDoctor(DoctorProfile doctor) { this.doctor = doctor; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public RequestedBy getRequestedBy() { return requestedBy; }
    public void setRequestedBy(RequestedBy requestedBy) { this.requestedBy = requestedBy; }

    public String getRequestNote() { return requestNote; }
    public void setRequestNote(String requestNote) { this.requestNote = requestNote; }

    public User getDecisionByUser() { return decisionByUser; }
    public void setDecisionByUser(User decisionByUser) { this.decisionByUser = decisionByUser; }

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
