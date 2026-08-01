package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.User;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO combiné User + PatientProfile, retourné par les endpoints /api/users/patients.
 */
public class PatientFullResponseDto {

    // ---- infos User ----
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private User.Gender gender;
    private LocalDate dateOfBirth;
    private String profilePhotoUrl;
    private String city;
    private String country;
    private boolean active;

    // ---- infos PatientProfile ----
    private UUID patientProfileId;
    private UUID assignedDoctorProfileId;
    private String medicalRecordNumber;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private Integer heightCm;
    private Double weightKg;
    private boolean medicalConsent;
    private Instant consentTimestamp;
    private Double lastKnownLatitude;
    private Double lastKnownLongitude;

    // ======== getters / setters ========

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public User.Gender getGender() { return gender; }
    public void setGender(User.Gender gender) { this.gender = gender; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public UUID getPatientProfileId() { return patientProfileId; }
    public void setPatientProfileId(UUID patientProfileId) { this.patientProfileId = patientProfileId; }

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

    public boolean isMedicalConsent() { return medicalConsent; }
    public void setMedicalConsent(boolean medicalConsent) { this.medicalConsent = medicalConsent; }

    public Instant getConsentTimestamp() { return consentTimestamp; }
    public void setConsentTimestamp(Instant consentTimestamp) { this.consentTimestamp = consentTimestamp; }

    public Double getLastKnownLatitude() { return lastKnownLatitude; }
    public void setLastKnownLatitude(Double lastKnownLatitude) { this.lastKnownLatitude = lastKnownLatitude; }

    public Double getLastKnownLongitude() { return lastKnownLongitude; }
    public void setLastKnownLongitude(Double lastKnownLongitude) { this.lastKnownLongitude = lastKnownLongitude; }
}
