package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.Allergy;
import jakarta.validation.constraints.Size;

public class AllergyUpdateRequestDto {

    @Size(max = 200)
    private String substance;

    @Size(max = 300)
    private String reaction;

    private Allergy.Severity severity;

    public String getSubstance() { return substance; }
    public void setSubstance(String substance) { this.substance = substance; }

    public String getReaction() { return reaction; }
    public void setReaction(String reaction) { this.reaction = reaction; }

    public Allergy.Severity getSeverity() { return severity; }
    public void setSeverity(Allergy.Severity severity) { this.severity = severity; }
}
