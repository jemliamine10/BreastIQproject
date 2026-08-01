package com.breastcancer.breastcancerbackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class PatientProfileCreateRequestDto {

   
    @Size(max = 80)
    private String medicalRecordNumber;

    @Size(max = 120)
    private String emergencyContactName;

    @Size(max = 30)
    private String emergencyContactPhone;

    private Integer heightCm;
    private Double weightKg;

    private boolean medicalConsent;
    private Instant consentTimestamp;

    // last known location
    private Double lastKnownLatitude;
    private Double lastKnownLongitude;

    // getters/setters
    
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

    public boolean isMedicalConsent() { return medicalConsent; }
    public void setMedicalConsent(boolean medicalConsent) { this.medicalConsent = medicalConsent; }

    public Instant getConsentTimestamp() { return consentTimestamp; }
    public void setConsentTimestamp(Instant consentTimestamp) { this.consentTimestamp = consentTimestamp; }

    public Double getLastKnownLatitude() { return lastKnownLatitude; }
    public void setLastKnownLatitude(Double lastKnownLatitude) { this.lastKnownLatitude = lastKnownLatitude; }

    public Double getLastKnownLongitude() { return lastKnownLongitude; }
    public void setLastKnownLongitude(Double lastKnownLongitude) { this.lastKnownLongitude = lastKnownLongitude; }
}
