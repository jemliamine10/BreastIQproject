package com.breastcancer.breastcancerbackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class LinkActionRequestDto {

    @NotNull
    private UUID linkId;

    private UUID decisionByUserId;

    @Size(max = 1000)
    private String rejectionReason;

    public UUID getLinkId() { return linkId; }
    public void setLinkId(UUID linkId) { this.linkId = linkId; }

    public UUID getDecisionByUserId() { return decisionByUserId; }
    public void setDecisionByUserId(UUID decisionByUserId) { this.decisionByUserId = decisionByUserId; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
