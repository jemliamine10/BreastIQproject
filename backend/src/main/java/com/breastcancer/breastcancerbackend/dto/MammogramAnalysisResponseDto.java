package com.breastcancer.breastcancerbackend.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO representing the full AI mammogram analysis response.
 * Contains detection status, annotated images, and per-detection details.
 */
public class MammogramAnalysisResponseDto {

    private boolean detections;

    private String fullImage;

    private String fullNormalImage;

    private String segmentationImage;

    private List<PredictionDto> individualPredictions;

    /** Overall confidence level (average of all detection scores, or 1.0 if no detections = normal) */
    private double globalConfidence;

    /** Summary label: "Normal", "Bénin", "Malin", "Mixte" */
    private String globalVerdict;

    /** ID of the persisted analysis (set after saving to DB) */
    private String analysisId;

    private String patientFirstName;
    private String patientLastName;

    public MammogramAnalysisResponseDto() {}

    // ── Getters & Setters ──

    public String getPatientFirstName() { return patientFirstName; }
    public void setPatientFirstName(String patientFirstName) { this.patientFirstName = patientFirstName; }

    public String getPatientLastName() { return patientLastName; }
    public void setPatientLastName(String patientLastName) { this.patientLastName = patientLastName; }

    public boolean isDetections() { return detections; }
    public void setDetections(boolean detections) { this.detections = detections; }

    public String getFullImage() { return fullImage; }
    public void setFullImage(String fullImage) { this.fullImage = fullImage; }

    public String getFullNormalImage() { return fullNormalImage; }
    public void setFullNormalImage(String fullNormalImage) { this.fullNormalImage = fullNormalImage; }

    public String getSegmentationImage() { return segmentationImage; }
    public void setSegmentationImage(String segmentationImage) { this.segmentationImage = segmentationImage; }

    public List<PredictionDto> getIndividualPredictions() { return individualPredictions; }
    public void setIndividualPredictions(List<PredictionDto> individualPredictions) {
        this.individualPredictions = individualPredictions;
    }

    public double getGlobalConfidence() { return globalConfidence; }
    public void setGlobalConfidence(double globalConfidence) { this.globalConfidence = globalConfidence; }

    public String getGlobalVerdict() { return globalVerdict; }
    public void setGlobalVerdict(String globalVerdict) { this.globalVerdict = globalVerdict; }

    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }

    /**
     * Individual detection prediction.
     */
    public static class PredictionDto {
        private String image;
        private String crop;
        private String label;
        private String classification;
        private double score;
        private Map<String, Object> features;

        public PredictionDto() {}

        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }

        public String getCrop() { return crop; }
        public void setCrop(String crop) { this.crop = crop; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public String getClassification() { return classification; }
        public void setClassification(String classification) { this.classification = classification; }

        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }

        public Map<String, Object> getFeatures() { return features; }
        public void setFeatures(Map<String, Object> features) { this.features = features; }
    }
}
