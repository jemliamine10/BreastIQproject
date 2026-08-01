package com.breastcancer.breastcancerbackend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "doctor_profiles",
        indexes = {
                @Index(name = "idx_doctor_license", columnList = "license_number"),
                @Index(name = "idx_doctor_verified", columnList = "is_verified"),
                @Index(name = "idx_doctor_type", columnList = "doctor_type")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_doctor_license", columnNames = {"license_number"})
        }
)
public class DoctorProfile {

    public enum ConsultationMode {
        IN_PERSON,
        VIDEO,
        HYBRID
    }

    // ✅ NEW: type de médecin (pour filtrage)
    public enum DoctorType {
        RADIOLOGIST,
        ONCOLOGIST,
        SURGEON,
        GYNECOLOGIST,
        PATHOLOGIST,
        RADIATION_ONCOLOGIST,
        GENERAL_PRACTITIONER,
        PSYCHOLOGIST,
        NURSE_NAVIGATOR,
        OTHER
    }

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_doctor_user"))
    private User user;

    // ✅ NEW: enum pour filtrage côté patient
    @Enumerated(EnumType.STRING)
    @Column(name = "doctor_type", nullable = false, length = 40)
    private DoctorType doctorType;

    // ✅ garder la spécialité (texte libre)
    @Column(name = "speciality", nullable = false, length = 120)
    private String speciality;

    @Column(name = "license_number", nullable = false, length = 80)
    private String licenseNumber;

    @Column(name = "clinic_name", length = 160)
    private String clinicName;

    @Column(name = "bio", length = 1500)
    private String bio;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    // ex: "fr,en,ar"
    @Column(name = "languages", length = 120)
    private String languages;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_mode", nullable = false, length = 20)
    private ConsultationMode consultationMode = ConsultationMode.IN_PERSON;

    @Column(name = "consultation_fee")
    private BigDecimal consultationFee;

    @Column(name = "is_verified", nullable = false)
    private boolean verified = false;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    // ===== Maps =====
    @Column(name = "address_text", length = 300)
    private String addressText;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = "UTC";

    // Relation : 1 médecin -> plusieurs patientes (si tu gardes assignedDoctor)
    @OneToMany(mappedBy = "assignedDoctor", fetch = FetchType.LAZY)
    private List<PatientProfile> patients = new ArrayList<>();

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    // ===== Getters & Setters =====
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public DoctorType getDoctorType() { return doctorType; }
    public void setDoctorType(DoctorType doctorType) { this.doctorType = doctorType; }

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

    public ConsultationMode getConsultationMode() { return consultationMode; }
    public void setConsultationMode(ConsultationMode consultationMode) { this.consultationMode = consultationMode; }

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

    public List<PatientProfile> getPatients() { return patients; }
    public void setPatients(List<PatientProfile> patients) { this.patients = patients; }
}
