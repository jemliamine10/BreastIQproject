package com.breastcancer.breastcancerbackend.dto;

import java.time.Instant;
import java.util.UUID;

public class TrackerEntryResponseDto {
    private UUID id;
    private UUID patientId;
    private Integer painLevel;
    private Integer fatigueLevel;
    private Integer moodLevel;
    private Double temperature;
    private Double weight;
    private boolean vomiting;
    private boolean diarrhea;
    private boolean appetiteLoss;
    private String notes;
    private Instant recordedAt;

    // Health score computed after this entry
    private Integer healthScore;
    private String riskLevel;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public Integer getPainLevel() { return painLevel; }
    public void setPainLevel(Integer pl) { this.painLevel = pl; }

    public Integer getFatigueLevel() { return fatigueLevel; }
    public void setFatigueLevel(Integer fl) { this.fatigueLevel = fl; }

    public Integer getMoodLevel() { return moodLevel; }
    public void setMoodLevel(Integer ml) { this.moodLevel = ml; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double t) { this.temperature = t; }

    public Double getWeight() { return weight; }
    public void setWeight(Double w) { this.weight = w; }

    public boolean isVomiting() { return vomiting; }
    public void setVomiting(boolean v) { this.vomiting = v; }

    public boolean isDiarrhea() { return diarrhea; }
    public void setDiarrhea(boolean d) { this.diarrhea = d; }

    public boolean isAppetiteLoss() { return appetiteLoss; }
    public void setAppetiteLoss(boolean a) { this.appetiteLoss = a; }

    public String getNotes() { return notes; }
    public void setNotes(String n) { this.notes = n; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant ra) { this.recordedAt = ra; }

    public Integer getHealthScore() { return healthScore; }
    public void setHealthScore(Integer hs) { this.healthScore = hs; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String rl) { this.riskLevel = rl; }
}
