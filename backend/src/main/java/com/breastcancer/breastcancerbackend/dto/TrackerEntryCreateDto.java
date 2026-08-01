package com.breastcancer.breastcancerbackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class TrackerEntryCreateDto {

    @NotNull
    private UUID patientId;

    @Min(0) @Max(10)
    private Integer painLevel;

    @Min(0) @Max(10)
    private Integer fatigueLevel;

    @Min(0) @Max(10)
    private Integer moodLevel;

    private Double temperature;
    private Double weight;
    private boolean vomiting;
    private boolean diarrhea;
    private boolean appetiteLoss;
    private String notes;

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

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
    public void setVomiting(boolean v) { this.vomiting = v; }

    public boolean isDiarrhea() { return diarrhea; }
    public void setDiarrhea(boolean d) { this.diarrhea = d; }

    public boolean isAppetiteLoss() { return appetiteLoss; }
    public void setAppetiteLoss(boolean a) { this.appetiteLoss = a; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
