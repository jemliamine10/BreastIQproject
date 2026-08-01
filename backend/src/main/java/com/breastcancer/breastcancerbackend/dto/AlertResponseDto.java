package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.Alert;
import java.time.Instant;
import java.util.UUID;

public class AlertResponseDto {
    private UUID id;
    private UUID patientId;
    private String patientName;
    private Alert.Severity severity;
    private Alert.AlertType alertType;
    private String message;
    private String triggerData;
    private boolean resolved;
    private Instant resolvedAt;
    private UUID resolvedBy;
    private String resolutionNotes;
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID pid) { this.patientId = pid; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String pn) { this.patientName = pn; }

    public Alert.Severity getSeverity() { return severity; }
    public void setSeverity(Alert.Severity s) { this.severity = s; }

    public Alert.AlertType getAlertType() { return alertType; }
    public void setAlertType(Alert.AlertType at) { this.alertType = at; }

    public String getMessage() { return message; }
    public void setMessage(String m) { this.message = m; }

    public String getTriggerData() { return triggerData; }
    public void setTriggerData(String td) { this.triggerData = td; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean r) { this.resolved = r; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant ra) { this.resolvedAt = ra; }

    public UUID getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(UUID rb) { this.resolvedBy = rb; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String rn) { this.resolutionNotes = rn; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant ca) { this.createdAt = ca; }
}
