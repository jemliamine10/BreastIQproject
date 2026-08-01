package com.breastcancer.breastcancerbackend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight DTO for the analysis history list — no base64 images.
 */
public class MammogramAnalysisHistoryDto {

    private UUID id;
    private UUID patientProfileId;
    private String patientFirstName;
    private String patientLastName;
    private Instant analysisDate;
    private String globalVerdict;
    private double globalConfidence;
    private int detectionsCount;
    private boolean hasReport;

    public MammogramAnalysisHistoryDto() {}

    // ── Getters & Setters ──

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPatientProfileId() { return patientProfileId; }
    public void setPatientProfileId(UUID patientProfileId) { this.patientProfileId = patientProfileId; }

    public String getPatientFirstName() { return patientFirstName; }
    public void setPatientFirstName(String patientFirstName) { this.patientFirstName = patientFirstName; }

    public String getPatientLastName() { return patientLastName; }
    public void setPatientLastName(String patientLastName) { this.patientLastName = patientLastName; }

    public Instant getAnalysisDate() { return analysisDate; }
    public void setAnalysisDate(Instant analysisDate) { this.analysisDate = analysisDate; }

    public String getGlobalVerdict() { return globalVerdict; }
    public void setGlobalVerdict(String globalVerdict) { this.globalVerdict = globalVerdict; }

    public double getGlobalConfidence() { return globalConfidence; }
    public void setGlobalConfidence(double globalConfidence) { this.globalConfidence = globalConfidence; }

    public int getDetectionsCount() { return detectionsCount; }
    public void setDetectionsCount(int detectionsCount) { this.detectionsCount = detectionsCount; }

    public boolean isHasReport() { return hasReport; }
    public void setHasReport(boolean hasReport) { this.hasReport = hasReport; }
}
