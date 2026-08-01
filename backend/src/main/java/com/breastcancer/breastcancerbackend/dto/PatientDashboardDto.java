package com.breastcancer.breastcancerbackend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PatientDashboardDto {

    // ── KPI cards ──
    private int healthScore;
    private String patientStatus;
    private int profileCompletion;
    private int activeTreatmentCount;
    private long unresolvedAlerts;
    private long unreadMessages;
    private long documentCount;

    // ── Next appointment ──
    private AppointmentInfo nextAppointment;

    // ── Tracker trend (line chart — last 7 days) ──
    private List<TrackerPoint> trackerTrend;

    // ── Current treatment progress ──
    private TreatmentProgress currentTreatment;

    // ── Recent timeline ──
    private List<TimelineItem> recentTimeline;

    // ── Recent alerts ──
    private List<AlertInfo> recentAlerts;

    // ═════ Inner classes ═════

    public static class AppointmentInfo {
        private UUID id;
        private String title;
        private Instant startAt;
        private Instant endAt;
        private String type;
        private String status;
        private String mode;
        private String doctorFirstName;
        private String doctorLastName;
        private String speciality;
        private String location;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
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
        public String getDoctorFirstName() { return doctorFirstName; }
        public void setDoctorFirstName(String doctorFirstName) { this.doctorFirstName = doctorFirstName; }
        public String getDoctorLastName() { return doctorLastName; }
        public void setDoctorLastName(String doctorLastName) { this.doctorLastName = doctorLastName; }
        public String getSpeciality() { return speciality; }
        public void setSpeciality(String speciality) { this.speciality = speciality; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }

    public static class TrackerPoint {
        private String date;
        private Integer painLevel;
        private Integer fatigueLevel;
        private Integer moodLevel;
        private Double temperature;

        public TrackerPoint() {}
        public TrackerPoint(String date, Integer painLevel, Integer fatigueLevel, Integer moodLevel, Double temperature) {
            this.date = date;
            this.painLevel = painLevel;
            this.fatigueLevel = fatigueLevel;
            this.moodLevel = moodLevel;
            this.temperature = temperature;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Integer getPainLevel() { return painLevel; }
        public void setPainLevel(Integer painLevel) { this.painLevel = painLevel; }
        public Integer getFatigueLevel() { return fatigueLevel; }
        public void setFatigueLevel(Integer fatigueLevel) { this.fatigueLevel = fatigueLevel; }
        public Integer getMoodLevel() { return moodLevel; }
        public void setMoodLevel(Integer moodLevel) { this.moodLevel = moodLevel; }
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
    }

    public static class TreatmentProgress {
        private UUID id;
        private String treatmentType;
        private String protocol;
        private String status;
        private int currentCycle;
        private int totalCycles;
        private String startDate;
        private String endDate;
        private long completedSessions;
        private long totalSessions;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getTreatmentType() { return treatmentType; }
        public void setTreatmentType(String treatmentType) { this.treatmentType = treatmentType; }
        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getCurrentCycle() { return currentCycle; }
        public void setCurrentCycle(int currentCycle) { this.currentCycle = currentCycle; }
        public int getTotalCycles() { return totalCycles; }
        public void setTotalCycles(int totalCycles) { this.totalCycles = totalCycles; }
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
        public long getCompletedSessions() { return completedSessions; }
        public void setCompletedSessions(long completedSessions) { this.completedSessions = completedSessions; }
        public long getTotalSessions() { return totalSessions; }
        public void setTotalSessions(long totalSessions) { this.totalSessions = totalSessions; }
    }

    public static class TimelineItem {
        private UUID id;
        private String eventType;
        private String title;
        private String description;
        private String severity;
        private Instant eventDate;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public Instant getEventDate() { return eventDate; }
        public void setEventDate(Instant eventDate) { this.eventDate = eventDate; }
    }

    public static class AlertInfo {
        private UUID id;
        private String severity;
        private String alertType;
        private String message;
        private Instant createdAt;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getAlertType() { return alertType; }
        public void setAlertType(String alertType) { this.alertType = alertType; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }

    // ═════ Root Getters & Setters ═════

    public int getHealthScore() { return healthScore; }
    public void setHealthScore(int healthScore) { this.healthScore = healthScore; }

    public String getPatientStatus() { return patientStatus; }
    public void setPatientStatus(String patientStatus) { this.patientStatus = patientStatus; }

    public int getProfileCompletion() { return profileCompletion; }
    public void setProfileCompletion(int profileCompletion) { this.profileCompletion = profileCompletion; }

    public int getActiveTreatmentCount() { return activeTreatmentCount; }
    public void setActiveTreatmentCount(int activeTreatmentCount) { this.activeTreatmentCount = activeTreatmentCount; }

    public long getUnresolvedAlerts() { return unresolvedAlerts; }
    public void setUnresolvedAlerts(long unresolvedAlerts) { this.unresolvedAlerts = unresolvedAlerts; }

    public long getUnreadMessages() { return unreadMessages; }
    public void setUnreadMessages(long unreadMessages) { this.unreadMessages = unreadMessages; }

    public long getDocumentCount() { return documentCount; }
    public void setDocumentCount(long documentCount) { this.documentCount = documentCount; }

    public AppointmentInfo getNextAppointment() { return nextAppointment; }
    public void setNextAppointment(AppointmentInfo nextAppointment) { this.nextAppointment = nextAppointment; }

    public List<TrackerPoint> getTrackerTrend() { return trackerTrend; }
    public void setTrackerTrend(List<TrackerPoint> trackerTrend) { this.trackerTrend = trackerTrend; }

    public TreatmentProgress getCurrentTreatment() { return currentTreatment; }
    public void setCurrentTreatment(TreatmentProgress currentTreatment) { this.currentTreatment = currentTreatment; }

    public List<TimelineItem> getRecentTimeline() { return recentTimeline; }
    public void setRecentTimeline(List<TimelineItem> recentTimeline) { this.recentTimeline = recentTimeline; }

    public List<AlertInfo> getRecentAlerts() { return recentAlerts; }
    public void setRecentAlerts(List<AlertInfo> recentAlerts) { this.recentAlerts = recentAlerts; }
}
