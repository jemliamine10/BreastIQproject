package com.breastcancer.breastcancerbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for risk prediction request — maps to the 22 METABRIC features.
 */
public class RiskPredictionRequestDto {

    @JsonProperty("age_at_diagnosis")
    private Double ageAtDiagnosis;

    @JsonProperty("type_of_breast_surgery")
    private String typeOfBreastSurgery = "Unknown";

    @JsonProperty("cellularity")
    private String cellularity = "Unknown";

    @JsonProperty("chemotherapy")
    private String chemotherapy = "Unknown";

    @JsonProperty("pam50_claudin_low_subtype")
    private String pam50SubType = "Unknown";

    @JsonProperty("er_status_measured_by_ihc")
    private String erStatusIhc = "Unknown";

    @JsonProperty("er_status")
    private String erStatus = "Unknown";

    @JsonProperty("neoplasm_histologic_grade")
    private Double neoplasmHistologicGrade = 2.0;

    @JsonProperty("her2_status_measured_by_snp6")
    private String her2StatusSnp6 = "Unknown";

    @JsonProperty("her2_status")
    private String her2Status = "Unknown";

    @JsonProperty("tumor_other_histologic_subtype")
    private String tumorHistologicSubtype = "Unknown";

    @JsonProperty("hormone_therapy")
    private String hormoneTherapy = "Unknown";

    @JsonProperty("inferred_menopausal_state")
    private String menopausalState = "Unknown";

    @JsonProperty("integrative_cluster")
    private String integrativeCluster = "Unknown";

    @JsonProperty("primary_tumor_laterality")
    private String tumorLaterality = "Unknown";

    @JsonProperty("lymph_nodes_examined_positive")
    private Double lymphNodesPositive = 0.0;

    @JsonProperty("mutation_count")
    private Double mutationCount = 5.0;

    @JsonProperty("nottingham_prognostic_index")
    private Double nottinghamIndex = 4.0;

    @JsonProperty("pr_status")
    private String prStatus = "Unknown";

    @JsonProperty("radio_therapy")
    private String radioTherapy = "Unknown";

    @JsonProperty("3_gene_classifier_subtype")
    private String threeGeneSubtype = "Unknown";

    @JsonProperty("tumor_size")
    private Double tumorSize = 25.0;

    @JsonProperty("tumor_stage")
    private Double tumorStage = 2.0;

    // ===== Getters & Setters =====

    public Double getAgeAtDiagnosis() { return ageAtDiagnosis; }
    public void setAgeAtDiagnosis(Double ageAtDiagnosis) { this.ageAtDiagnosis = ageAtDiagnosis; }

    public String getTypeOfBreastSurgery() { return typeOfBreastSurgery; }
    public void setTypeOfBreastSurgery(String typeOfBreastSurgery) { this.typeOfBreastSurgery = typeOfBreastSurgery; }

    public String getCellularity() { return cellularity; }
    public void setCellularity(String cellularity) { this.cellularity = cellularity; }

    public String getChemotherapy() { return chemotherapy; }
    public void setChemotherapy(String chemotherapy) { this.chemotherapy = chemotherapy; }

    public String getPam50SubType() { return pam50SubType; }
    public void setPam50SubType(String pam50SubType) { this.pam50SubType = pam50SubType; }

    public String getErStatusIhc() { return erStatusIhc; }
    public void setErStatusIhc(String erStatusIhc) { this.erStatusIhc = erStatusIhc; }

    public String getErStatus() { return erStatus; }
    public void setErStatus(String erStatus) { this.erStatus = erStatus; }

    public Double getNeoplasmHistologicGrade() { return neoplasmHistologicGrade; }
    public void setNeoplasmHistologicGrade(Double neoplasmHistologicGrade) { this.neoplasmHistologicGrade = neoplasmHistologicGrade; }

    public String getHer2StatusSnp6() { return her2StatusSnp6; }
    public void setHer2StatusSnp6(String her2StatusSnp6) { this.her2StatusSnp6 = her2StatusSnp6; }

    public String getHer2Status() { return her2Status; }
    public void setHer2Status(String her2Status) { this.her2Status = her2Status; }

    public String getTumorHistologicSubtype() { return tumorHistologicSubtype; }
    public void setTumorHistologicSubtype(String tumorHistologicSubtype) { this.tumorHistologicSubtype = tumorHistologicSubtype; }

    public String getHormoneTherapy() { return hormoneTherapy; }
    public void setHormoneTherapy(String hormoneTherapy) { this.hormoneTherapy = hormoneTherapy; }

    public String getMenopausalState() { return menopausalState; }
    public void setMenopausalState(String menopausalState) { this.menopausalState = menopausalState; }

    public String getIntegrativeCluster() { return integrativeCluster; }
    public void setIntegrativeCluster(String integrativeCluster) { this.integrativeCluster = integrativeCluster; }

    public String getTumorLaterality() { return tumorLaterality; }
    public void setTumorLaterality(String tumorLaterality) { this.tumorLaterality = tumorLaterality; }

    public Double getLymphNodesPositive() { return lymphNodesPositive; }
    public void setLymphNodesPositive(Double lymphNodesPositive) { this.lymphNodesPositive = lymphNodesPositive; }

    public Double getMutationCount() { return mutationCount; }
    public void setMutationCount(Double mutationCount) { this.mutationCount = mutationCount; }

    public Double getNottinghamIndex() { return nottinghamIndex; }
    public void setNottinghamIndex(Double nottinghamIndex) { this.nottinghamIndex = nottinghamIndex; }

    public String getPrStatus() { return prStatus; }
    public void setPrStatus(String prStatus) { this.prStatus = prStatus; }

    public String getRadioTherapy() { return radioTherapy; }
    public void setRadioTherapy(String radioTherapy) { this.radioTherapy = radioTherapy; }

    public String getThreeGeneSubtype() { return threeGeneSubtype; }
    public void setThreeGeneSubtype(String threeGeneSubtype) { this.threeGeneSubtype = threeGeneSubtype; }

    public Double getTumorSize() { return tumorSize; }
    public void setTumorSize(Double tumorSize) { this.tumorSize = tumorSize; }

    public Double getTumorStage() { return tumorStage; }
    public void setTumorStage(Double tumorStage) { this.tumorStage = tumorStage; }
}
