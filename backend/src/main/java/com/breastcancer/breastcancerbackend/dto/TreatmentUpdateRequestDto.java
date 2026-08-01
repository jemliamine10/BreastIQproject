package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.Treatment;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class TreatmentUpdateRequestDto {

    private Treatment.TreatmentType treatmentType;

    @Size(max = 120)
    private String protocol;

    @Size(max = 200)
    private String medicationName;

    @Size(max = 100)
    private String dosage;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer cyclesTotal;

    private Treatment.Status status;

    @Size(max = 1200)
    private String notes;

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

    public Treatment.Status getStatus() { return status; }
    public void setStatus(Treatment.Status status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
