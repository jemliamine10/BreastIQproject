package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.Treatment;

import java.time.LocalDate;
import java.util.UUID;

public class TreatmentResponseDto {

    private UUID id;
    private UUID patientProfileId;

    private Treatment.TreatmentType treatmentType;
    private String protocol;
    private String medicationName;
    private String dosage;

    private LocalDate startDate;
    private LocalDate endDate;

    private Integer cyclesTotal;
    private Integer currentCycle;

    private Treatment.Status status;
    private String notes;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPatientProfileId() { return patientProfileId; }
    public void setPatientProfileId(UUID patientProfileId) { this.patientProfileId = patientProfileId; }

    public Treatment.TreatmentType getTreatmentType() { return treatmentType; }
    public void setTreatmentType(Treatment.TreatmentType treatmentType) { this.treatmentType = treatmentType; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getCyclesTotal() { return cyclesTotal; }
    public void setCyclesTotal(Integer cyclesTotal) { this.cyclesTotal = cyclesTotal; }

    public Integer getCurrentCycle() { return currentCycle; }
    public void setCurrentCycle(Integer currentCycle) { this.currentCycle = currentCycle; }

    public Treatment.Status getStatus() { return status; }
    public void setStatus(Treatment.Status status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
