package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.PatientProfile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PatientProfileResponseDto {

    private UUID id;
    private UUID userId;

    private UUID assignedDoctorProfileId;

    private String medicalRecordNumber;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private Integer heightCm;
    private Double weightKg;
    private Double bmi;

    // Clinical monitoring
    private String bloodType;
    private Integer healthScore;
    private PatientProfile.PatientStatus patientStatus;

    private boolean medicalConsent;
    private Instant consentTimestamp;

    private Double lastKnownLatitude;
    private Double lastKnownLongitude;

    private List<AllergyResponseDto> allergies;
    private List<TreatmentResponseDto> treatments;

    // getters/setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getAssignedDoctorProfileId() { return assignedDoctorProfileId; }
    public void setAssignedDoctorProfileId(UUID assignedDoctorProfileId) { this.assignedDoctorProfileId = assignedDoctorProfileId; }

    public String getMedicalRecordNumber() { return medicalRecordNumber; }
    public void setMedicalRecordNumber(String medicalRecordNumber) { this.medicalRecordNumber = medicalRecordNumber; }

    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }

    public Integer getHeightCm() { return heightCm; }
    public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public Double getBmi() { return bmi; }
    public void setBmi(Double bmi) { this.bmi = bmi; }

    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }

    public Integer getHealthScore() { return healthScore; }
    public void setHealthScore(Integer healthScore) { this.healthScore = healthScore; }

    public PatientProfile.PatientStatus getPatientStatus() { return patientStatus; }
    public void setPatientStatus(PatientProfile.PatientStatus patientStatus) { this.patientStatus = patientStatus; }

    public boolean isMedicalConsent() { return medicalConsent; }
    public void setMedicalConsent(boolean medicalConsent) { this.medicalConsent = medicalConsent; }

    public Instant getConsentTimestamp() { return consentTimestamp; }
    public void setConsentTimestamp(Instant consentTimestamp) { this.consentTimestamp = consentTimestamp; }

    public Double getLastKnownLatitude() { return lastKnownLatitude; }
    public void setLastKnownLatitude(Double lastKnownLatitude) { this.lastKnownLatitude = lastKnownLatitude; }

    public Double getLastKnownLongitude() { return lastKnownLongitude; }
    public void setLastKnownLongitude(Double lastKnownLongitude) { this.lastKnownLongitude = lastKnownLongitude; }

    public List<AllergyResponseDto> getAllergies() { return allergies; }
    public void setAllergies(List<AllergyResponseDto> allergies) { this.allergies = allergies; }

    public List<TreatmentResponseDto> getTreatments() { return treatments; }
    public void setTreatments(List<TreatmentResponseDto> treatments) { this.treatments = treatments; }
}
