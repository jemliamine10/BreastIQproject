package com.breastcancer.breastcancerbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for risk prediction response from the Python FastAPI microservice.
 */
public class RiskPredictionResponseDto {

    @JsonProperty("probability")
    private Double probability;

    @JsonProperty("probability_percent")
    private Double probabilityPercent;

    @JsonProperty("is_high_risk")
    private Boolean isHighRisk;

    @JsonProperty("risk_level")
    private String riskLevel;

    @JsonProperty("threshold")
    private Double threshold;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("features_used")
    private Integer featuresUsed;

    @JsonProperty("model_metrics")
    private ModelMetrics modelMetrics;

    // Nested class for model metrics
    public static class ModelMetrics {
        @JsonProperty("f1")
        private Double f1;

        @JsonProperty("auc")
        private Double auc;

        public Double getF1() { return f1; }
        public void setF1(Double f1) { this.f1 = f1; }

        public Double getAuc() { return auc; }
        public void setAuc(Double auc) { this.auc = auc; }
    }

    // ===== Getters & Setters =====

    public Double getProbability() { return probability; }
    public void setProbability(Double probability) { this.probability = probability; }

    public Double getProbabilityPercent() { return probabilityPercent; }
    public void setProbabilityPercent(Double probabilityPercent) { this.probabilityPercent = probabilityPercent; }

    public Boolean getIsHighRisk() { return isHighRisk; }
    public void setIsHighRisk(Boolean isHighRisk) { this.isHighRisk = isHighRisk; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public Double getThreshold() { return threshold; }
    public void setThreshold(Double threshold) { this.threshold = threshold; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public Integer getFeaturesUsed() { return featuresUsed; }
    public void setFeaturesUsed(Integer featuresUsed) { this.featuresUsed = featuresUsed; }

    public ModelMetrics getModelMetrics() { return modelMetrics; }
    public void setModelMetrics(ModelMetrics modelMetrics) { this.modelMetrics = modelMetrics; }
}
