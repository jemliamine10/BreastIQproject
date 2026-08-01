package com.breastcancer.breastcancerbackend.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DoctorDashboardDto {

    // ── KPI cards ──
    private long totalPatients;
    private long pendingRequests;
    private long unresolvedAlerts;
    private long criticalAlerts;
    private long appointmentsToday;
    private double avgHealthScore;
    private long unreadMessages;

    // ── Distributions (for charts) ──
    private Map<String, Long> statusDistribution;   // STABLE / WARNING / CRITICAL
    private Map<String, Long> stageDistribution;     // STAGE_0..IV
    private Map<String, Long> treatmentDistribution; // CHEMO / RADIO / SURGERY / HORMONAL / IMMUNOTHERAPY

    // ── Health trend (line chart) ──
    private List<HealthTrendPoint> healthTrend;

    // ── Recent alerts ──
    private List<AlertSummary> recentAlerts;

    // ── Today's appointments ──
    private List<AppointmentSummary> todayAppointments;

    // ── Top patients needing attention ──
    private List<PatientSummary> criticalPatients;

    // ═════ Inner classes ═════

    public static class HealthTrendPoint {
        private String date;
        private double avgScore;

        public HealthTrendPoint() {}
        public HealthTrendPoint(String date, double avgScore) {
            this.date = date;
            this.avgScore = avgScore;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public double getAvgScore() { return avgScore; }
        public void setAvgScore(double avgScore) { this.avgScore = avgScore; }
    }

    public static class AlertSummary {
        private UUID id;
        private String severity;
        private String alertType;
        private String message;
        private String patientName;
        private UUID patientProfileId;
        private Instant createdAt;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getAlertType() { return alertType; }
        public void setAlertType(String alertType) { this.alertType = alertType; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getPatientName() { return patientName; }
        public void setPatientName(String patientName) { this.patientName = patientName; }
        public UUID getPatientProfileId() { return patientProfileId; }
        public void setPatientProfileId(UUID patientProfileId) { this.patientProfileId = patientProfileId; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }

    public static class AppointmentSummary {
        private UUID id;
        private String title;
        private String patientName;
        private UUID patientProfileId;
        private Instant startAt;
        private Instant endAt;
        private String type;
        private String status;
        private String mode;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getPatientName() { return patientName; }
        public void setPatientName(String patientName) { this.patientName = patientName; }
        public UUID getPatientProfileId() { return patientProfileId; }
        public void setPatientProfileId(UUID patientProfileId) { this.patientProfileId = patientProfileId; }
        public Instant getStartAt() { return startAt; }
        public void setStartAt(Instant startAt) { this.startAt = startAt; }
        public Instant getEndAt() { return endAt; }
        public void setEndAt(Instant endAt) { this.endAt = endAt; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
    }

    public static class PatientSummary {
        private UUID patientProfileId;
        private String firstName;
        private String lastName;
        private int healthScore;
        private String patientStatus;
        private String activeTreatment;
        private String cancerStage;

        public UUID getPatientProfileId() { return patientProfileId; }
        public void setPatientProfileId(UUID patientProfileId) { this.patientProfileId = patientProfileId; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public int getHealthScore() { return healthScore; }
        public void setHealthScore(int healthScore) { this.healthScore = healthScore; }
        public String getPatientStatus() { return patientStatus; }
        public void setPatientStatus(String patientStatus) { this.patientStatus = patientStatus; }
        public String getActiveTreatment() { return activeTreatment; }
        public void setActiveTreatment(String activeTreatment) { this.activeTreatment = activeTreatment; }
        public String getCancerStage() { return cancerStage; }
        public void setCancerStage(String cancerStage) { this.cancerStage = cancerStage; }
    }

    // ═════ Root Getters & Setters ═════

    public long getTotalPatients() { return totalPatients; }
    public void setTotalPatients(long totalPatients) { this.totalPatients = totalPatients; }

    public long getPendingRequests() { return pendingRequests; }
    public void setPendingRequests(long pendingRequests) { this.pendingRequests = pendingRequests; }

    public long getUnresolvedAlerts() { return unresolvedAlerts; }
    public void setUnresolvedAlerts(long unresolvedAlerts) { this.unresolvedAlerts = unresolvedAlerts; }

    public long getCriticalAlerts() { return criticalAlerts; }
    public void setCriticalAlerts(long criticalAlerts) { this.criticalAlerts = criticalAlerts; }

    public long getAppointmentsToday() { return appointmentsToday; }
    public void setAppointmentsToday(long appointmentsToday) { this.appointmentsToday = appointmentsToday; }

    public double getAvgHealthScore() { return avgHealthScore; }
    public void setAvgHealthScore(double avgHealthScore) { this.avgHealthScore = avgHealthScore; }

    public long getUnreadMessages() { return unreadMessages; }
    public void setUnreadMessages(long unreadMessages) { this.unreadMessages = unreadMessages; }

    public Map<String, Long> getStatusDistribution() { return statusDistribution; }
    public void setStatusDistribution(Map<String, Long> statusDistribution) { this.statusDistribution = statusDistribution; }

    public Map<String, Long> getStageDistribution() { return stageDistribution; }
    public void setStageDistribution(Map<String, Long> stageDistribution) { this.stageDistribution = stageDistribution; }

    public Map<String, Long> getTreatmentDistribution() { return treatmentDistribution; }
    public void setTreatmentDistribution(Map<String, Long> treatmentDistribution) { this.treatmentDistribution = treatmentDistribution; }

    public List<HealthTrendPoint> getHealthTrend() { return healthTrend; }
    public void setHealthTrend(List<HealthTrendPoint> healthTrend) { this.healthTrend = healthTrend; }

    public List<AlertSummary> getRecentAlerts() { return recentAlerts; }
    public void setRecentAlerts(List<AlertSummary> recentAlerts) { this.recentAlerts = recentAlerts; }

    public List<AppointmentSummary> getTodayAppointments() { return todayAppointments; }
    public void setTodayAppointments(List<AppointmentSummary> todayAppointments) { this.todayAppointments = todayAppointments; }

    public List<PatientSummary> getCriticalPatients() { return criticalPatients; }
    public void setCriticalPatients(List<PatientSummary> criticalPatients) { this.criticalPatients = criticalPatients; }
}
