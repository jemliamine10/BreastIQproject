package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.MedicalRecord;
import com.breastcancer.breastcancerbackend.entity.PatientProfile;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class MedicalRecordCreateDto {

    @NotNull
    private UUID patientId;

    private String diagnosis;
    private MedicalRecord.CancerStage cancerStage;
    private MedicalRecord.TumorType tumorType;
    private boolean consentGiven;
    private String notes;

    // Optional: patient health info to update simultaneously
    private PatientProfile.BloodType bloodType;
    private Integer heightCm;
    private Double weightKg;

    // Optional: clinical data to create alongside
    private ClinicalDataDto clinicalData;

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

    public PatientProfile.BloodType getBloodType() { return bloodType; }
    public void setBloodType(PatientProfile.BloodType bloodType) { this.bloodType = bloodType; }

    public Integer getHeightCm() { return heightCm; }
    public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public ClinicalDataDto getClinicalData() { return clinicalData; }
    public void setClinicalData(ClinicalDataDto clinicalData) { this.clinicalData = clinicalData; }
}
