package com.breastcancer.breastcancerbackend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(
        name = "treatments",
        indexes = {
                @Index(name = "idx_treatment_patient", columnList = "patient_id"),
                @Index(name = "idx_treatment_status", columnList = "status")
        }
)
public class Treatment {

    public enum TreatmentType {
        CHEMO,
        RADIO,
        SURGERY,
        HORMONAL,
        IMMUNOTHERAPY
    }

    public enum Status {
        UPCOMING,
        ACTIVE,
        COMPLETED,
        STOPPED;

        @JsonCreator
        public static Status fromValue(String value) {
            if (value == null) {
                return null;
            }

            String normalized = value.trim().toUpperCase(Locale.ROOT);
            if ("ONGOING".equals(normalized)) {
                return ACTIVE;
            }
            return Status.valueOf(normalized);
        }
    }

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_treatment_patient"))
    private PatientProfile patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "treatment_type", nullable = false, length = 30)
    private TreatmentType treatmentType;

    @Column(name = "protocol", length = 120)
    private String protocol; // e.g. "AC-T", "TCH"

    @Column(name = "medication_name", length = 200)
    private String medicationName;

    @Column(name = "dosage", length = 100)
    private String dosage;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "cycles_total")
    private Integer cyclesTotal;

    @Column(name = "current_cycle")
    private Integer currentCycle = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.UPCOMING;

    @Column(name = "notes", length = 1200)
    private String notes;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    // ===== Sessions =====
    @OneToMany(mappedBy = "treatment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TreatmentSession> sessions = new ArrayList<>();

    /**
     * Auto-calculate treatment status based on dates and session progress.
     * Called before business logic needs the current status.
     */
    @Transient
    public Status getComputedStatus() {
        if (this.status == Status.STOPPED) return Status.STOPPED;
        LocalDate today = LocalDate.now();
        if (startDate != null && today.isBefore(startDate)) return Status.UPCOMING;
        if (endDate != null && today.isAfter(endDate)) return Status.COMPLETED;
        if (cyclesTotal != null && currentCycle != null && currentCycle >= cyclesTotal) return Status.COMPLETED;
        if (startDate != null && !today.isBefore(startDate)) return Status.ACTIVE;
        return this.status;
    }

    /**
     * Syncs the persisted status with the computed one.
     */
    public void refreshStatus() {
        Status computed = getComputedStatus();
        if (this.status != Status.STOPPED) {
            this.status = computed;
        }
    }

    // ===== Getters & Setters =====
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PatientProfile getPatient() { return patient; }
    public void setPatient(PatientProfile patient) { this.patient = patient; }

    public TreatmentType getTreatmentType() { return treatmentType; }
    public void setTreatmentType(TreatmentType treatmentType) { this.treatmentType = treatmentType; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getCyclesTotal() { return cyclesTotal; }
    public void setCyclesTotal(Integer cyclesTotal) { this.cyclesTotal = cyclesTotal; }

    public Integer getCurrentCycle() { return currentCycle; }
    public void setCurrentCycle(Integer currentCycle) { this.currentCycle = currentCycle; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public List<TreatmentSession> getSessions() { return sessions; }
    public void setSessions(List<TreatmentSession> sessions) { this.sessions = sessions; }
}
