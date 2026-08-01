package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.MedicalHistory;
import java.time.LocalDate;
import java.util.UUID;

public class MedicalHistoryDto {

    private UUID id;
    private UUID patientId;
    private MedicalHistory.HistoryType historyType;
    private String title;
    private String description;
    private LocalDate eventDate;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public MedicalHistory.HistoryType getHistoryType() { return historyType; }
    public void setHistoryType(MedicalHistory.HistoryType historyType) { this.historyType = historyType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
}
