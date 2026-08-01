package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.DoctorProfile;
import com.breastcancer.breastcancerbackend.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO combiné User + DoctorProfile, retourné par les endpoints /api/users/doctors.
 */
public class DoctorFullResponseDto {

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

    // ---- infos DoctorProfile ----
    private UUID doctorProfileId;
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

    public UUID getDoctorProfileId() { return doctorProfileId; }
    public void setDoctorProfileId(UUID doctorProfileId) { this.doctorProfileId = doctorProfileId; }

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
}
