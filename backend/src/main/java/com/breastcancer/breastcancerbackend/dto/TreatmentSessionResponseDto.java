package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.TreatmentSession;
import java.time.LocalDate;
import java.util.UUID;

public class TreatmentSessionResponseDto {
    private UUID id;
    private UUID treatmentId;
    private Integer sessionNumber;
    private LocalDate scheduledDate;
    private LocalDate actualDate;
    private TreatmentSession.SessionStatus status;
    private String notes;
    private String sideEffects;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTreatmentId() { return treatmentId; }
    public void setTreatmentId(UUID treatmentId) { this.treatmentId = treatmentId; }

    public Integer getSessionNumber() { return sessionNumber; }
    public void setSessionNumber(Integer sn) { this.sessionNumber = sn; }

    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate sd) { this.scheduledDate = sd; }

    public LocalDate getActualDate() { return actualDate; }
    public void setActualDate(LocalDate ad) { this.actualDate = ad; }

    public TreatmentSession.SessionStatus getStatus() { return status; }
    public void setStatus(TreatmentSession.SessionStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getSideEffects() { return sideEffects; }
    public void setSideEffects(String sideEffects) { this.sideEffects = sideEffects; }
}
