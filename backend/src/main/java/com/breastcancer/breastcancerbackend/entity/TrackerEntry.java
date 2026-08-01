package com.breastcancer.breastcancerbackend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "tracker_entries",
        indexes = {
                @Index(name = "idx_tracker_patient", columnList = "patient_id"),
                @Index(name = "idx_tracker_recorded", columnList = "recorded_at")
        }
)
public class TrackerEntry {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tracker_patient"))
    private PatientProfile patient;

    @Column(name = "pain_level")
    private Integer painLevel; // 0-10

    @Column(name = "fatigue_level")
    private Integer fatigueLevel; // 0-10

    @Column(name = "mood_level")
    private Integer moodLevel; // 0-10 (10 = best mood)

    @Column(name = "temperature")
    private Double temperature; // in Celsius

    @Column(name = "weight")
    private Double weight; // in kg

    @Column(name = "vomiting", nullable = false)
    private boolean vomiting = false;

    @Column(name = "diarrhea", nullable = false)
    private boolean diarrhea = false;

    @Column(name = "appetite_loss", nullable = false)
    private boolean appetiteLoss = false;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt = Instant.now();

    // ===== Getters & Setters =====
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PatientProfile getPatient() { return patient; }
    public void setPatient(PatientProfile patient) { this.patient = patient; }

    public Integer getPainLevel() { return painLevel; }
    public void setPainLevel(Integer painLevel) { this.painLevel = painLevel; }

    public Integer getFatigueLevel() { return fatigueLevel; }
    public void setFatigueLevel(Integer fatigueLevel) { this.fatigueLevel = fatigueLevel; }

    public Integer getMoodLevel() { return moodLevel; }
    public void setMoodLevel(Integer moodLevel) { this.moodLevel = moodLevel; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public boolean isVomiting() { return vomiting; }
    public void setVomiting(boolean vomiting) { this.vomiting = vomiting; }

    public boolean isDiarrhea() { return diarrhea; }
    public void setDiarrhea(boolean diarrhea) { this.diarrhea = diarrhea; }

    public boolean isAppetiteLoss() { return appetiteLoss; }
    public void setAppetiteLoss(boolean appetiteLoss) { this.appetiteLoss = appetiteLoss; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
}
