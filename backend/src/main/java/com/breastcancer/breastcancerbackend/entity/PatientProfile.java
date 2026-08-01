package com.breastcancer.breastcancerbackend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "patient_profiles",
        indexes = {
                @Index(name = "idx_patient_assigned_doctor", columnList = "assigned_doctor_id"),
                @Index(name = "idx_patient_status", columnList = "patient_status")
        }
)
public class PatientProfile {

    public enum PatientStatus {
        STABLE,
        WARNING,
        CRITICAL
    }

    public enum BloodType {
        A_POSITIVE, A_NEGATIVE,
        B_POSITIVE, B_NEGATIVE,
        AB_POSITIVE, AB_NEGATIVE,
        O_POSITIVE, O_NEGATIVE
    }

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_patient_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_doctor_id",
            foreignKey = @ForeignKey(name = "fk_patient_assigned_doctor"))
    private DoctorProfile assignedDoctor;

    @Column(name = "medical_record_number", length = 80)
    private String medicalRecordNumber;

    @Column(name = "emergency_contact_name", length = 120)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 30)
    private String emergencyContactPhone;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "profile_completion")
    private Integer profileCompletion = 0;

    // ===== Clinical monitoring =====
    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type", length = 20)
    private BloodType bloodType;

    @Column(name = "health_score")
    private Integer healthScore = 100;

    @Enumerated(EnumType.STRING)
    @Column(name = "patient_status", length = 20)
    private PatientStatus patientStatus = PatientStatus.STABLE;

    // Consent
    @Column(name = "medical_consent", nullable = false)
    private boolean medicalConsent = false;

    @Column(name = "consent_timestamp")
    private Instant consentTimestamp;

    // ===== Location =====
    @Column(name = "last_known_latitude")
    private Double lastKnownLatitude;

    @Column(name = "last_known_longitude")
    private Double lastKnownLongitude;

    // ===== Relationships =====
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Allergy> allergies = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Treatment> treatments = new ArrayList<>();

    @OneToOne(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private MedicalRecord medicalRecord;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MedicalHistory> medicalHistories = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TrackerEntry> trackerEntries = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Alert> alerts = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MedicalEvent> medicalEvents = new ArrayList<>();

    // ===== Computed =====
    @Transient
    public Double getBmi() {
        if (heightCm == null || heightCm <= 0 || weightKg == null || weightKg <= 0) return null;
        double heightM = heightCm / 100.0;
        return Math.round((weightKg / (heightM * heightM)) * 100.0) / 100.0;
    }

    public boolean hasLocation() {
        return lastKnownLatitude != null && lastKnownLongitude != null;
    }

    @Transient
    public Double distanceToAssignedDoctorKm() {
        if (assignedDoctor == null || !this.hasLocation() || !assignedDoctor.hasLocation()) return null;
        return haversineKm(lastKnownLatitude, lastKnownLongitude,
                assignedDoctor.getLatitude(), assignedDoctor.getLongitude());
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0088;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // ===== Getters & Setters =====
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public DoctorProfile getAssignedDoctor() { return assignedDoctor; }
    public void setAssignedDoctor(DoctorProfile assignedDoctor) { this.assignedDoctor = assignedDoctor; }

    public String getMedicalRecordNumber() { return medicalRecordNumber; }
    public void setMedicalRecordNumber(String mrn) { this.medicalRecordNumber = mrn; }

    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String n) { this.emergencyContactName = n; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String p) { this.emergencyContactPhone = p; }

    public Integer getHeightCm() { return heightCm; }
    public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public Integer getProfileCompletion() { return profileCompletion; }
    public void setProfileCompletion(Integer pc) { this.profileCompletion = pc; }

    public BloodType getBloodType() { return bloodType; }
    public void setBloodType(BloodType bloodType) { this.bloodType = bloodType; }

    public Integer getHealthScore() { return healthScore; }
    public void setHealthScore(Integer healthScore) { this.healthScore = healthScore; }

    public PatientStatus getPatientStatus() { return patientStatus; }
    public void setPatientStatus(PatientStatus patientStatus) { this.patientStatus = patientStatus; }

    public boolean isMedicalConsent() { return medicalConsent; }
    public void setMedicalConsent(boolean medicalConsent) {
        this.medicalConsent = medicalConsent;
        this.consentTimestamp = medicalConsent ? Instant.now() : null;
    }

    public Instant getConsentTimestamp() { return consentTimestamp; }
    public void setConsentTimestamp(Instant consentTimestamp) { this.consentTimestamp = consentTimestamp; }

    public Double getLastKnownLatitude() { return lastKnownLatitude; }
    public void setLastKnownLatitude(Double lat) { this.lastKnownLatitude = lat; }

    public Double getLastKnownLongitude() { return lastKnownLongitude; }
    public void setLastKnownLongitude(Double lon) { this.lastKnownLongitude = lon; }

    public List<Allergy> getAllergies() { return allergies; }
    public void setAllergies(List<Allergy> allergies) { this.allergies = allergies; }

    public List<Treatment> getTreatments() { return treatments; }
    public void setTreatments(List<Treatment> treatments) { this.treatments = treatments; }

    public MedicalRecord getMedicalRecord() { return medicalRecord; }
    public void setMedicalRecord(MedicalRecord medicalRecord) { this.medicalRecord = medicalRecord; }

    public List<MedicalHistory> getMedicalHistories() { return medicalHistories; }
    public void setMedicalHistories(List<MedicalHistory> mh) { this.medicalHistories = mh; }

    public List<TrackerEntry> getTrackerEntries() { return trackerEntries; }
    public void setTrackerEntries(List<TrackerEntry> te) { this.trackerEntries = te; }

    public List<Alert> getAlerts() { return alerts; }
    public void setAlerts(List<Alert> alerts) { this.alerts = alerts; }

    public List<MedicalEvent> getMedicalEvents() { return medicalEvents; }
    public void setMedicalEvents(List<MedicalEvent> me) { this.medicalEvents = me; }
}
