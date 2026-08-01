package com.breastcancer.breastcancerbackend.dto;

import java.util.UUID;

public class RiskAssessmentDto {

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    private UUID patientId;
    private int riskScore; // 0-100
    private RiskLevel riskLevel;
    private String summary;

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
