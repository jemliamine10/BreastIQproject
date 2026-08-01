package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.ClinicalData;
import com.breastcancer.breastcancerbackend.entity.MedicalRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class MedicalRecordResponseDto {

    private UUID id;
    private UUID patientId;
    private String diagnosis;
    private MedicalRecord.CancerStage cancerStage;
    private MedicalRecord.TumorType tumorType;
    private boolean consentGiven;
    private String notes;

    // From PatientProfile
    private String bloodType;
    private Integer heightCm;
    private Double weightKg;
    private Double bmi;

    // Nested clinical data
    private ClinicalDataDto clinicalData;

    // Medical histories
    private List<MedicalHistoryDto> medicalHistories;

    // Allergies
    private List<AllergyResponseDto> allergies;

    // Treatments
    private List<TreatmentResponseDto> treatments;

    // TNM staging
    private String tnmClassification;
    private boolean stageAutoComputed;
    private String computedStageLabel;

    private Instant createdAt;
    private Instant updatedAt;

    // ===== Getters & Setters =====
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public MedicalRecord.CancerStage getCancerStage() { return cancerStage; }
    public void setCancerStage(MedicalRecord.CancerStage cancerStage) { this.cancerStage = cancerStage; }

    public MedicalRecord.TumorType getTumorType() { return tumorType; }
    public void setTumorType(MedicalRecord.TumorType tumorType) { this.tumorType = tumorType; }

    public boolean isConsentGiven() { return consentGiven; }
    public void setConsentGiven(boolean consentGiven) { this.consentGiven = consentGiven; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }

    public Integer getHeightCm() { return heightCm; }
    public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public Double getBmi() { return bmi; }
    public void setBmi(Double bmi) { this.bmi = bmi; }

    public ClinicalDataDto getClinicalData() { return clinicalData; }
    public void setClinicalData(ClinicalDataDto clinicalData) { this.clinicalData = clinicalData; }

    public List<MedicalHistoryDto> getMedicalHistories() { return medicalHistories; }
    public void setMedicalHistories(List<MedicalHistoryDto> medicalHistories) { this.medicalHistories = medicalHistories; }

    public List<AllergyResponseDto> getAllergies() { return allergies; }
    public void setAllergies(List<AllergyResponseDto> allergies) { this.allergies = allergies; }

    public List<TreatmentResponseDto> getTreatments() { return treatments; }
    public void setTreatments(List<TreatmentResponseDto> treatments) { this.treatments = treatments; }

    public String getTnmClassification() { return tnmClassification; }
    public void setTnmClassification(String tnmClassification) { this.tnmClassification = tnmClassification; }

    public boolean isStageAutoComputed() { return stageAutoComputed; }
    public void setStageAutoComputed(boolean stageAutoComputed) { this.stageAutoComputed = stageAutoComputed; }

    public String getComputedStageLabel() { return computedStageLabel; }
    public void setComputedStageLabel(String computedStageLabel) { this.computedStageLabel = computedStageLabel; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
