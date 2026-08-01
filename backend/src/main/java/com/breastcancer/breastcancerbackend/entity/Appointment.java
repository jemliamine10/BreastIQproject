package com.breastcancer.breastcancerbackend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "appointments",
        indexes = {
                @Index(name = "idx_appt_link", columnList = "link_id"),
                @Index(name = "idx_appt_start", columnList = "start_at"),
                @Index(name = "idx_appt_status", columnList = "status")
        }
)
public class Appointment {

    public enum Status {
        UPCOMING,
        REQUESTED,   // créé par la patiente
        CONFIRMED,   // confirmé par médecin
        CANCELLED,
        COMPLETED,
        NO_SHOW
    }

    public enum AppointmentType {
        CONSULTATION,
        EXAM,
        TREATMENT,
        FOLLOW_UP,
        OTHER
    }

    public enum Mode {
        IN_PERSON,
        VIDEO
    }

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // 🔥 Point clé: l’appointment est rattaché à une "connexion" patient-médecin
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "link_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_appt_link"))
    private PatientDoctorLink link;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private AppointmentType type = AppointmentType.CONSULTATION;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "description", length = 1200)
    private String description;

    @Column(name = "location", length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private Mode mode = Mode.IN_PERSON;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.REQUESTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescheduled_from_id", foreignKey = @ForeignKey(name = "fk_appt_rescheduled_from"))
    private Appointment rescheduledFrom;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "patient_notes", length = 1200)
    private String patientNotes;

    @Column(name = "doctor_notes", length = 1200)
    private String doctorNotes;

    @ElementCollection
    @CollectionTable(name = "appointment_notes", joinColumns = @JoinColumn(name = "appointment_id"))
    @Column(name = "note", length = 500)
    private List<String> notes = new ArrayList<>();

    // Optionnel : lien téléconsultation
    @Column(name = "video_room_url", length = 800)
    private String videoRoomUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Helpers (très utiles)
    @Transient
    public PatientProfile getPatient() {
        return link != null ? link.getPatient() : null;
    }

    @Transient
    public DoctorProfile getDoctor() {
        return link != null ? link.getDoctor() : null;
    }

    // ===== Getters & Setters =====
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PatientDoctorLink getLink() { return link; }
    public void setLink(PatientDoctorLink link) { this.link = link; }

    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }

    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }

    public AppointmentType getType() { return type; }
    public void setType(AppointmentType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Appointment getRescheduledFrom() { return rescheduledFrom; }
    public void setRescheduledFrom(Appointment rescheduledFrom) { this.rescheduledFrom = rescheduledFrom; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getPatientNotes() { return patientNotes; }
    public void setPatientNotes(String patientNotes) { this.patientNotes = patientNotes; }

    public String getDoctorNotes() { return doctorNotes; }
    public void setDoctorNotes(String doctorNotes) { this.doctorNotes = doctorNotes; }

    public List<String> getNotes() { return notes; }
    public void setNotes(List<String> notes) { this.notes = notes; }

    public String getVideoRoomUrl() { return videoRoomUrl; }
    public void setVideoRoomUrl(String videoRoomUrl) { this.videoRoomUrl = videoRoomUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
