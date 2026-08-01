package com.breastcancer.breastcancerbackend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "medical_records",
        indexes = {
                @Index(name = "idx_medrec_patient", columnList = "patient_id")
        }
)
public class MedicalRecord {

    public enum CancerStage {
        STAGE_0,
        STAGE_I,
        STAGE_II,
        STAGE_III,
        STAGE_IV
    }

    public enum TumorType {
        HR_POSITIVE,          // Hormone Receptor Positive
        HER2_POSITIVE,        // HER2 Positive
        TRIPLE_NEGATIVE,      // Triple-Negative
        HR_POSITIVE_HER2_POSITIVE, // Both
        UNKNOWN
    }

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_medrec_patient"))
    private PatientProfile patient;

    @Column(name = "diagnosis", length = 300)
    private String diagnosis;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancer_stage", length = 20)
    private CancerStage cancerStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "tumor_type", length = 40)
    private TumorType tumorType;

    @Column(name = "consent_given", nullable = false)
    private boolean consentGiven = false;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "tnm_classification", length = 20)
    private String tnmClassification;

    @Column(name = "stage_auto_computed", nullable = false)
    private boolean stageAutoComputed = false;


    // ===== Related clinical data =====
    @OneToOne(mappedBy = "medicalRecord", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ClinicalData clinicalData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }

    // ===== Getters & Setters =====
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PatientProfile getPatient() { return patient; }
    public void setPatient(PatientProfile patient) { this.patient = patient; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public CancerStage getCancerStage() { return cancerStage; }
    public void setCancerStage(CancerStage cancerStage) { this.cancerStage = cancerStage; }

    public TumorType getTumorType() { return tumorType; }
    public void setTumorType(TumorType tumorType) { this.tumorType = tumorType; }

    public boolean isConsentGiven() { return consentGiven; }
    public void setConsentGiven(boolean consentGiven) { this.consentGiven = consentGiven; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public ClinicalData getClinicalData() { return clinicalData; }
    public void setClinicalData(ClinicalData clinicalData) { this.clinicalData = clinicalData; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getTnmClassification() { return tnmClassification; }
    public void setTnmClassification(String tnmClassification) { this.tnmClassification = tnmClassification; }

    public boolean isStageAutoComputed() { return stageAutoComputed; }
    public void setStageAutoComputed(boolean stageAutoComputed) { this.stageAutoComputed = stageAutoComputed; }
}
