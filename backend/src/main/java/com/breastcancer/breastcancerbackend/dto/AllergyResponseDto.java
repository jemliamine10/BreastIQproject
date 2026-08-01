package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.Allergy;

import java.util.UUID;

public class AllergyResponseDto {

    private UUID id;
    private UUID patientProfileId;
    private String substance;
    private String reaction;
    private Allergy.Severity severity;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPatientProfileId() { return patientProfileId; }
    public void setPatientProfileId(UUID patientProfileId) { this.patientProfileId = patientProfileId; }

    public String getSubstance() { return substance; }
    public void setSubstance(String substance) { this.substance = substance; }

    public String getReaction() { return reaction; }
    public void setReaction(String reaction) { this.reaction = reaction; }

    public Allergy.Severity getSeverity() { return severity; }
    public void setSeverity(Allergy.Severity severity) { this.severity = severity; }
}
