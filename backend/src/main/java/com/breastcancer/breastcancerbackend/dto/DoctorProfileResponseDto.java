package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.DoctorProfile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class DoctorProfileResponseDto {

    private UUID id;
    private UUID userId;

    // ✅ NEW
    private DoctorProfile.DoctorType doctorType;

    private String speciality;
    private String licenseNumber;
    private String clinicName;
    private String bio;
    private Integer yearsOfExperience;
    private String languages;

    private DoctorProfile.ConsultationMode consultationMode;
    private BigDecimal consultationFee;

    private boolean verified;
    private Instant verifiedAt;

    private String addressText;
    private Double latitude;
    private Double longitude;
    private String timezone;

    // getters/setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public DoctorProfile.DoctorType getDoctorType() { return doctorType; }
    public void setDoctorType(DoctorProfile.DoctorType doctorType) { this.doctorType = doctorType; }

    public String getSpeciality() { return speciality; }
    public void setSpeciality(String speciality) { this.speciality = speciality; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getClinicName() { return clinicName; }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public Integer getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(Integer yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public String getLanguages() { return languages; }
    public void setLanguages(String languages) { this.languages = languages; }

    public DoctorProfile.ConsultationMode getConsultationMode() { return consultationMode; }
    public void setConsultationMode(DoctorProfile.ConsultationMode consultationMode) { this.consultationMode = consultationMode; }

    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getAddressText() { return addressText; }
    public void setAddressText(String addressText) { this.addressText = addressText; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
}
