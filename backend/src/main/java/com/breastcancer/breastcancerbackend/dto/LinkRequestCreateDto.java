package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.PatientDoctorLink;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class LinkRequestCreateDto {

    private UUID patientProfileId;

    // Alias frontend
    private UUID patientId;

    private UUID doctorProfileId;

    // Alias frontend
    private UUID doctorId;

    @NotNull
    private PatientDoctorLink.RequestedBy requestedBy;

    @Size(max = 1000)
    private String requestNote;

    public UUID getPatientProfileId() { return patientProfileId; }
    public void setPatientProfileId(UUID patientProfileId) { this.patientProfileId = patientProfileId; }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public UUID getDoctorProfileId() { return doctorProfileId; }
    public void setDoctorProfileId(UUID doctorProfileId) { this.doctorProfileId = doctorProfileId; }

    public UUID getDoctorId() { return doctorId; }
    public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }

    public PatientDoctorLink.RequestedBy getRequestedBy() { return requestedBy; }
    public void setRequestedBy(PatientDoctorLink.RequestedBy requestedBy) { this.requestedBy = requestedBy; }

    public String getRequestNote() { return requestNote; }
    public void setRequestNote(String requestNote) { this.requestNote = requestNote; }
}
