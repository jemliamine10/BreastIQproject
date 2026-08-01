package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.DoctorProfile;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class DoctorProfileUpdateRequestDto {

    // ✅ NEW (optionnel)
    private DoctorProfile.DoctorType doctorType;

    @Size(max = 120)
    private String speciality;

    @Size(max = 160)
    private String clinicName;

    @Size(max = 1500)
    private String bio;

    private Integer yearsOfExperience;

    @Size(max = 120)
    private String languages;

    private DoctorProfile.ConsultationMode consultationMode;

    private BigDecimal consultationFee;

    // Maps
    @Size(max = 300)
    private String addressText;

    private Double latitude;
    private Double longitude;
    private String timezone;

    private Boolean verified;

    // getters/setters
    public DoctorProfile.DoctorType getDoctorType() { return doctorType; }
    public void setDoctorType(DoctorProfile.DoctorType doctorType) { this.doctorType = doctorType; }

    public String getSpeciality() { return speciality; }
    public void setSpeciality(String speciality) { this.speciality = speciality; }

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

    public String getAddressText() { return addressText; }
    public void setAddressText(String addressText) { this.addressText = addressText; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
}
