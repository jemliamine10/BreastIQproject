package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.PatientDoctorLink;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class LinkDecisionDto {

    @NotNull
    private PatientDoctorLink.Status newStatus; // ACTIVE / REJECTED / BLOCKED / ENDED

    @NotNull
    private UUID decisionByUserId;

    @Size(max = 1000)
    private String rejectionReason; // utile si REJECTED/BLOCKED

    public PatientDoctorLink.Status getNewStatus() { return newStatus; }
    public void setNewStatus(PatientDoctorLink.Status newStatus) { this.newStatus = newStatus; }

    public UUID getDecisionByUserId() { return decisionByUserId; }
    public void setDecisionByUserId(UUID decisionByUserId) { this.decisionByUserId = decisionByUserId; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
