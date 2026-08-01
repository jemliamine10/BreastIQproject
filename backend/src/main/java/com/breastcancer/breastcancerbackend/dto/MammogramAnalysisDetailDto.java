package com.breastcancer.breastcancerbackend.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Detailed DTO for viewing a specific mammogram analysis with images and report.
 */
public class MammogramAnalysisDetailDto {

    private UUID id;
    private UUID patientProfileId;
    private String patientFirstName;
    private String patientLastName;
    private UUID doctorProfileId;
    private Instant analysisDate;
    private String globalVerdict;
    private double globalConfidence;
    private int detectionsCount;

    // Base64 images reloaded from disk
    private String fullImage;
    private String fullNormalImage;
    private String segmentationImage;

    // Parsed predictions
    private List<MammogramAnalysisResponseDto.PredictionDto> individualPredictions;

    // AI Report
    private String aiReport;
    private Instant reportGeneratedAt;

    public MammogramAnalysisDetailDto() {}

    // ── Getters & Setters ──

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPatientProfileId() { return patientProfileId; }
    public void setPatientProfileId(UUID patientProfileId) { this.patientProfileId = patientProfileId; }

    public String getPatientFirstName() { return patientFirstName; }
    public void setPatientFirstName(String patientFirstName) { this.patientFirstName = patientFirstName; }

    public String getPatientLastName() { return patientLastName; }
    public void setPatientLastName(String patientLastName) { this.patientLastName = patientLastName; }

    public UUID getDoctorProfileId() { return doctorProfileId; }
    public void setDoctorProfileId(UUID doctorProfileId) { this.doctorProfileId = doctorProfileId; }

    public Instant getAnalysisDate() { return analysisDate; }
    public void setAnalysisDate(Instant analysisDate) { this.analysisDate = analysisDate; }

    public String getGlobalVerdict() { return globalVerdict; }
    public void setGlobalVerdict(String globalVerdict) { this.globalVerdict = globalVerdict; }

    public double getGlobalConfidence() { return globalConfidence; }
    public void setGlobalConfidence(double globalConfidence) { this.globalConfidence = globalConfidence; }

    public int getDetectionsCount() { return detectionsCount; }
    public void setDetectionsCount(int detectionsCount) { this.detectionsCount = detectionsCount; }

    public String getFullImage() { return fullImage; }
    public void setFullImage(String fullImage) { this.fullImage = fullImage; }

    public String getFullNormalImage() { return fullNormalImage; }
    public void setFullNormalImage(String fullNormalImage) { this.fullNormalImage = fullNormalImage; }

    public String getSegmentationImage() { return segmentationImage; }
    public void setSegmentationImage(String segmentationImage) { this.segmentationImage = segmentationImage; }

    public List<MammogramAnalysisResponseDto.PredictionDto> getIndividualPredictions() { return individualPredictions; }
    public void setIndividualPredictions(List<MammogramAnalysisResponseDto.PredictionDto> individualPredictions) {
        this.individualPredictions = individualPredictions;
    }

    public String getAiReport() { return aiReport; }
    public void setAiReport(String aiReport) { this.aiReport = aiReport; }

    public Instant getReportGeneratedAt() { return reportGeneratedAt; }
    public void setReportGeneratedAt(Instant reportGeneratedAt) { this.reportGeneratedAt = reportGeneratedAt; }
}
